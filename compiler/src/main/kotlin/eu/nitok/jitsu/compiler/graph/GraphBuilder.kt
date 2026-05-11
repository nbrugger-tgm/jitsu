package eu.nitok.jitsu.compiler.graph


import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Position
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.analysis.AnalysisRepository
import eu.nitok.jitsu.compiler.graph.Constant.*
import eu.nitok.jitsu.compiler.graph.Expression.ArrayLiteral
import eu.nitok.jitsu.compiler.graph.Expression.VariableReference
import eu.nitok.jitsu.compiler.graph.Type.*
import eu.nitok.jitsu.compiler.graph.Type.Array
import eu.nitok.jitsu.compiler.graph.Type.Boolean
import eu.nitok.jitsu.compiler.graph.Type.Float
import eu.nitok.jitsu.compiler.graph.Type.Int
import eu.nitok.jitsu.compiler.graph.TypeDefinition.ParameterizedType.Struct.Field
import eu.nitok.jitsu.compiler.graph.TypeDefinition.TypeParameter
import eu.nitok.jitsu.parser.ast.*
import eu.nitok.jitsu.parser.ast.StatementNode.*
import eu.nitok.jitsu.parser.ast.StatementNode.Declaration.FunctionDeclarationNode
import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.*
import java.net.URI

private class GraphBuilder {
    val messages: CompilerMessages = CompilerMessages()
}

/**
 * Restores a de-serialized Module to its former inter-connected state
 */
fun restoreJitsuModule(module: JitsuModule, dependencies: List<JitsuModule> = listOf()) {
    val moduleLookup = merge(*(dependencies + module).map { it.moduleLookup }.toTypedArray()) { a, b ->
        if (!(a.files.isEmpty() && b.files.isEmpty()))
            module.messages.error(
                "Module name conflict, ${a.name}, ${b.name}",
                Position(1, 1, URI((a.files.getOrNull(0) ?: b.files[0]).path))
            )
        a
    }
    populateBackReferences(module, module.messages, moduleLookup)
    module.sequence().forEach {
        if (it is AccessImpl<*>) it.restore(module.messages)
    }
}

fun buildJitsuModule(moduleAst: JitsuModuleAst, dependencies: List<JitsuModule> = listOf()): JitsuModule {
    return GraphBuilder().buildGraph(moduleAst, dependencies)
}

fun buildJitsuModule(file: SourceFileNode, dependencies: List<JitsuModule> = listOf()): JitsuModule {
    return GraphBuilder().buildGraph(
        JitsuModuleAst(singleFileModuleName(file.url), listOf(file), listOf()),
        dependencies
    )
}

private fun singleFileModuleName(file: URI): String {
    val parts = file.path.split("/")
    return parts.last().replace(".jit", "")
}

private fun GraphBuilder.buildGraph(moduleAst: JitsuModuleAst, dependencies: List<JitsuModule>): JitsuModule {
    val messages = CompilerMessages()
    val typeDb: IrStore<TypeDefinition> = IrStore()
    val functionDb: IrStore<Function> = IrStore()
    val variableDb: IrStore<Variable> = IrStore()

    fun buildModule(
        prefix: String?,
        module: JitsuModuleAst,
        typeDb: IrStore<TypeDefinition> = IrStore(),
        functionDb: IrStore<Function> = IrStore(),
        variableDb: IrStore<Variable> = IrStore()
    ): JitsuModule {
        val moduleName = prefix?.let { "$it.${module.name}" } ?: module.name
        val subModules = module.modules.map { buildModule(moduleName, it) }
        val files = module.files.map { buildGraph(it, module.name, typeDb, functionDb, variableDb) }
        val module = JitsuModule(moduleName, files, subModules, typeDb, functionDb, variableDb)
        return module
    }

    val module = buildModule(null, moduleAst, typeDb, functionDb, variableDb)

    val moduleLookup = merge(*(dependencies + module).map { it.moduleLookup }.toTypedArray()) { a, b ->
        if (!(a.files.isEmpty() && b.files.isEmpty()))
            messages.error(
                "Module name conflict, ${a.name}, ${b.name}",
                Position(1, 1, URI((a.files.getOrNull(0) ?: b.files[0]).path))
            )
        a
    }

    //backreferences
    populateBackReferences(module, messages, moduleLookup)
    module.sequence().forEach {
        if (it is Access.TypeAccess) {
            it.resolve(messages)
        }
    }

    val repository = AnalysisRepository()
    val allFiles = module.allModules.flatMap { it.files }.toList()
    val topLevelFunctions = allFiles.flatMap { it.functions }
    repository.analyzeAll(topLevelFunctions, messages)
    allFiles.asSequence().flatMap { it.sequence() }.forEach {
        if (it is Function) {
            it.summary = repository.getFunctionSummary(it)
        }
    }
    module.messages.add(messages)
    return module
}

private fun populateBackReferences(
    module: JitsuModule,
    messages: CompilerMessages,
    moduleLookup: Map<String, JitsuModule>
) {
    module.setScopes()
    module.sequence().forEach {
        if (it is Import) {
            it.resolve(messages, moduleLookup)
        }
    }
}

private fun GraphBuilder.buildGraph(
    srcFile: SourceFileNode, module: String,
    typeDb: IrStore<TypeDefinition> = IrStore(),
    functionDb: IrStore<Function> = IrStore(),
    variableDb: IrStore<Variable> = IrStore()
): JitsuFile {
    val statements = processStatements(srcFile.statements, module) {
        if (it !is Declaration) messages.error("Statement not allowed at root level", it.location)
    }
    return JitsuFile(
        functions = statements.functions,
        types = statements.types,
        variables = statements.variables,
        imports = statements.imports,
        path = srcFile.url.toString(), //TODO: toString should not be needed
        typeDb = typeDb,
        functionDb = functionDb,
        variableDb = variableDb
    )
}

data class Statements(
    val functions: List<Function>,
    val variables: List<VariableDeclaration>,
    val constants: List<Constant<Any>>,
    val types: List<TypeDefinition>,
    val imports: List<Import>
)

private fun GraphBuilder.processStatements(
    statements: List<StatementNode>,
    module: String?,
    instructionHandler: (InstructionNode) -> Unit
): Statements {
    val functions: MutableList<Function> = mutableListOf()
    val variables: MutableList<VariableDeclaration> = mutableListOf()
    val constants: MutableList<Constant<Any>> = mutableListOf()
    val types: MutableList<TypeDefinition> = mutableListOf()
    val imports = mutableMapOf<String, Import>()
    val allowImports = module != null
    for (statement in statements) {
        when (statement) {
            is NamedTypeDeclarationNode.EnumDeclarationNode -> types.add(buildGraph(statement))
            is NamedTypeDeclarationNode.TypeAliasNode -> buildGraph(statement)?.let { types.add(it) }
            is NamedTypeDeclarationNode.InterfaceTypeNode -> types.add(buildGraph(statement))
            is FunctionDeclarationNode -> {
                functions.add(buildFunctionGraph(statement))
                instructionHandler(statement)
            }

            is VariableDeclarationNode -> {
                variables.add(buildGraph(statement))
                instructionHandler(statement)
            }

            is InstructionNode -> instructionHandler(statement)
            is NamedTypeDeclarationNode.ClassDeclarationNode -> buildClassGraph(statement)?.let { types.add(it) }
            is Declaration.ImportNode -> {
                if (allowImports) messages.error("This scope does not (yet) allow imports", statement)
                else if (imports[statement.moduleReference.value] != null) messages.warn(
                    "Duplicated import", statement.location, CompilerMessage.Hint(
                        "First occurence", imports[statement.moduleReference.value]!!
                    )
                ) else if (statement.moduleReference.value == module) messages.error(
                    "A module cannot import itself",
                    statement.location
                )
                else imports[statement.moduleReference.value] = Import(statement.moduleReference)
            }
        }
    }
    return Statements(functions, variables, constants, types, imports.values.toList())
}

private fun GraphBuilder.buildClassGraph(classNode: NamedTypeDeclarationNode.ClassDeclarationNode): TypeDefinition.ParameterizedType.Class? {
    return classNode.name?.let { name ->
        val fields = classNode.fields.map { field ->
            Field(
                field.name.located,
                field.mutableKw != null,
                resolveType(field.type)
            )
        }
        return TypeDefinition.ParameterizedType.Class(
            name.located,
            classNode.typeParameters.map { buildTypeParameterGraph(it) },
            fields,
            classNode.methods.map {
                val base = buildFunctionGraph(it.function);
                Function(
                    base.name,
                    base.returnType,
                    listOf(
                        Function.Parameter(
                            Located<String>("this", name.location),
                            TypeReference(name.located, listOf()),
                            null
                        )
                    ) + base.parameters,
                    base.body,
                    base.location
                )
            }
        )
    }
}

private fun GraphBuilder.buildInstructionGraph(statement: InstructionNode): Instruction? {
    return when (statement) {
        is IfNode,
        is MethodInvocationNode,
        is AssignmentNode,
        is CodeBlockNode,
        is YieldStatement,
        is SwitchNode -> TODO();


        is FunctionCallNode -> Instruction.FunctionCall(
            statement.function.located,
            statement.parameters.map { buildExpressionGraph(it) },
            statement.location
        )

        is ReturnNode -> Instruction.Return(
            statement.expression?.let { node -> buildExpressionGraph(node) },
            statement.keywordLocation
        )

        is VariableDeclarationNode -> buildGraph(statement)
        is LineCommentNode -> null;
        is FunctionDeclarationNode -> buildFunctionGraph(statement)
    }
}

fun buildGraph(statement: VariableDeclarationNode): VariableDeclaration {
    val explicitType = statement.type?.let { resolveType(it) }
    val initialValue = statement.value?.let { node -> buildExpressionGraph(node) }
    val variableDeclaration = VariableDeclaration(
        false,
        statement.name?.located ?: Located<String>("unnamed", statement.keywordLocation),
        explicitType,
        initialValue
    )
    return variableDeclaration
}

fun buildGraph(statement: NamedTypeDeclarationNode.TypeAliasNode): TypeDefinition.ParameterizedType.Alias? {
    return statement.name?.let {
        TypeDefinition.ParameterizedType.Alias(
            it.located,
            statement.typeParameters.map { buildTypeParameterGraph(it) },
            resolveType(statement.type)
        )
    }
}

private fun buildGraph(
    it: NamedTypeDeclarationNode.InterfaceTypeNode
) = TypeDefinition.ParameterizedType.Interface(
    it.name.located,
    listOf(),
    it.functions.map { func -> NamedFunctionSignature(func.name.located, buildGraph(func.typeSignature)) }
)

private fun buildGraph(
    it: TypeNode.FunctionTypeSignatureNode
) = FunctionTypeSignature(
    it.returnType?.let { resolveType(it) },
    it.parameters.map {
        val type = resolveType(it.type);
        FunctionTypeSignature.Parameter(it.name.located, type, false)
    }
)

private fun buildGraph(enum: NamedTypeDeclarationNode.EnumDeclarationNode): TypeDefinition.DirectTypeDefinition.Enum {
    return TypeDefinition.DirectTypeDefinition.Enum(
        enum.name.located,
        enum.constants.map { TypeDefinition.DirectTypeDefinition.Enum.Constant(it.located) })
}

private fun buildTypeParameterGraph(node: IdentifierNode): TypeParameter = TypeParameter(node.located)

private fun buildGraph(
    array: TypeNode.ArrayTypeNode
) = Array(
    resolveType(array.type),
    array.fixedSize?.value?.toInt()
)

fun buildExpressionGraph(expression: ExpressionNode): Expression {
    return when (expression) {
        is ExpressionNode.BooleanLiteralNode -> BooleanConstant(expression.value, expression.location)
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> TODO()
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> resolveIntConstant(expression)
        is ExpressionNode.OperationNode -> {
            val left = buildExpressionGraph(expression.left)
            val right = expression.right?.let { buildExpressionGraph(it) }
                ?: Expression.Undefined(expression.operator.location)
            val operator = expression.operator
            Instruction.FunctionCall(
                operator.map { it.functionName },
                listOf(left, right),
                left.location.rangeTo(right.location),
            )
        }

        is ExpressionNode.StringLiteralNode -> StringConstant(expression.toString(), expression.location)
        is ExpressionNode.VariableReferenceNode -> resolveVariableReference(expression)
        is ExpressionNode.FieldAccessNode -> TODO()
        is ExpressionNode.IndexAccessNode -> TODO()
        is CodeBlockNode.SingleExpressionCodeBlock -> TODO()
        is CodeBlockNode.StatementsCodeBlock -> TODO()
        is FunctionCallNode -> resolveFunctionCall(expression)
        is FunctionDeclarationNode -> TODO()
        is IfNode -> TODO()
        is MethodInvocationNode -> TODO()
        is SwitchNode -> TODO()
        is ExpressionNode.ArrayLiteralNode -> ArrayLiteral(
            expression.elements.map { buildExpressionGraph(it) },
            expression.location
        )
    }
}

fun resolveFunctionCall(expression: FunctionCallNode): Instruction.FunctionCall {
    return Instruction.FunctionCall(
        expression.function.located,
        expression.parameters.map { buildExpressionGraph(it) },
        expression.location
    )
}

fun resolveVariableReference(
    expression: ExpressionNode.VariableReferenceNode
): Expression {
    return VariableReference(expression.variable.located)
}

private fun GraphBuilder.buildFunctionGraph(functionNode: FunctionDeclarationNode): Function {
    val name = functionNode.name
    val parameters = functionNode.parameters.map {
        val type = resolveType(it.type)
        Function.Parameter(
            it.name.located, type,
            it.defaultValue?.let { buildExpressionGraph(it) }
        )
    }
    val functionBody = when (val body = functionNode.body) {
        is CodeBlockNode.SingleExpressionCodeBlock -> TODO()//buildExpressionGraph(body.expression, function.bodyScope)
        is CodeBlockNode.StatementsCodeBlock -> Function.Body.Implementation(buildCodeBlockGraph(body.statements))
        is FunctionDeclarationNode.FunctionBodyNode.NativeImplementation -> Function.Body.Native(
            //TODO: resolve attribute name, this is just a fallback if no name is given
            "jitsu_native_${name}${
                parameters.joinToString("_", prefix = "_") {
                    it.type.toString().replace(Regex("[^0-9a-bA-B]"), "_")
                }
            }",
        )

        null -> Function.Body.Missing
    }
    return Function(
        name?.located,
        functionNode.returnType?.let { Located(resolveType(it), it.location) },
        parameters,
        functionBody,
        functionNode.name?.location ?: functionNode.keywordLocation
    );
}

private fun GraphBuilder.buildCodeBlockGraph(statements: List<StatementNode>): CodeBlock {
    val instructions = mutableListOf<Instruction>()
    processStatements(statements, null) {
        val instruction = buildInstructionGraph(it)
        if (instruction != null) instructions.add(instruction)
    }
    return CodeBlock(instructions)
}

fun resolveConstantOperation(scope: Scope, expression: ExpressionNode.OperationNode): Constant<Any>? {
//    val left = resolveConstant(scope, expression.left, null)
//    val right = resolveConstant(scope, expression.right, null)
//    if (left == null || right == null) return null;
//    val operator = expression.operator
//    return when (operator) {
//        BiOperator.ADDITION -> when (left) {
//            is Constant.StringConstant -> Constant.StringConstant(left.value + right.value.toString())
//            is Constant.BooleanConstant -> if (right is Constant.StringConstant) Constant.StringConstant(left.value.toString() + right.value) else {
//                scope.errors.add(Scope.Error("Cannot add boolean to ${right::class.simpleName}", expression.location))
//                null
//            }
//
//            is Constant.IntConstant -> when (right) {
//                is Constant.IntConstant -> Constant.IntConstant(left.value + right.value)
//                is Constant.UIntConstant -> Constant.IntConstant(right.value.toLong() + left.value)
//
//                is Constant.StringConstant -> Constant.StringConstant(left.value.toString() + right.value)
//                else -> {
//                    scope.errors.add(
//                        Scope.Error(
//                            "Cannot add integer to ${right::class.simpleName}",
//                            expression.location
//                        )
//                    )
//                    null
//                }
//            }
//
//            is Constant.UIntConstant -> when (right) {
//                is Constant.IntConstant -> Constant.IntConstant(left.value.toLong() + right.value)
//                is Constant.UIntConstant -> Constant.UIntConstant(right.value + left.value)
//
//                is Constant.StringConstant -> Constant.StringConstant(left.value.toString() + right.value)
//                else -> {
//                    scope.errors.add(
//                        Scope.Error(
//                            "Cannot add integer to ${right::class.simpleName}",
//                            expression.location
//                        )
//                    )
//                    null
//                }
//            }
//        }
//
//        BiOperator.SUBTRACTION, BiOperator.MODULO, BiOperator.MULTIPLICATION, BiOperator.DIVISION -> {
//            val leftInt = when (left) {
//                is Constant.IntConstant -> left.value.toBigInteger()
//                is Constant.UIntConstant -> left.value.toLong().toBigInteger()
//                else -> {
//                    scope.errors.add(
//                        Scope.Error("Cannot use math on ${left::class.simpleName}", expression.location)
//                    )
//                    return null;
//                }
//            };
//            val rightInt = when (right) {
//                is Constant.IntConstant -> right.value.toBigInteger()
//                is Constant.UIntConstant -> right.value.toLong().toBigInteger()
//                else -> {
//                    scope.errors.add(
//                        Scope.Error("Cannot use math on ${right::class.simpleName}", expression.location)
//                    )
//                    return null;
//                }
//            };
//            return when (operator) {
//                BiOperator.SUBTRACTION -> Constant.IntConstant((leftInt - rightInt).toLong())
//                BiOperator.MODULO -> Constant.IntConstant((leftInt % rightInt).toLong())
//                BiOperator.MULTIPLICATION -> Constant.IntConstant((leftInt * rightInt).toLong())
//                BiOperator.DIVISION -> Constant.IntConstant((leftInt / rightInt).toLong())
//                else -> throw IllegalStateException("Unreachable")
//            }
//        }
//
//        BiOperator.GREATER, BiOperator.LESS, BiOperator.GREATER_EQUAL, BiOperator.LESS_EQUAL -> {
//            val leftInt = when (left) {
//                is Constant.IntConstant -> left.value.toBigInteger()
//                is Constant.UIntConstant -> left.value.toLong().toBigInteger()
//                else -> {
//                    scope.errors.add(
//                        Scope.Error("Cannot use math on ${left::class.simpleName}", expression.location)
//                    )
//                    return null;
//                }
//            };
//            val rightInt = when (right) {
//                is Constant.IntConstant -> right.value.toBigInteger()
//                is Constant.UIntConstant -> right.value.toLong().toBigInteger()
//                else -> {
//                    scope.errors.add(
//                        Scope.Error("Cannot use math on ${right::class.simpleName}", expression.location)
//                    )
//                    return null;
//                }
//            };
//            return Constant.BooleanConstant(
//                when (operator) {
//                    BiOperator.GREATER -> leftInt > rightInt
//                    BiOperator.LESS -> leftInt < rightInt
//                    BiOperator.GREATER_EQUAL -> leftInt >= rightInt
//                    BiOperator.LESS_EQUAL -> leftInt <= rightInt
//                    else -> throw IllegalStateException("Unreachable")
//                }
//            )
//        }
//
//        BiOperator.AND -> if (left is Constant.BooleanConstant && right is Constant.BooleanConstant) {
//            Constant.BooleanConstant(left.value && right.value)
//        } else {
//            scope.errors.add(
//                Scope.Error(
//                    "Cannot use AND on ${left::class.simpleName} and ${right::class.simpleName}",
//                    expression.location
//                )
//            )
//            null
//        }
//
//        BiOperator.OR -> if (left is Constant.BooleanConstant && right is Constant.BooleanConstant) {
//            Constant.BooleanConstant(left.value || right.value)
//        } else {
//            scope.errors.add(
//                Scope.Error(
//                    "Cannot use OR on ${left::class.simpleName} and ${right::class.simpleName}",
//                    expression.location
//                )
//            )
//            null
//        }
//    }
    TODO();
}

private fun resolveIntConstant(
    expression: ExpressionNode.NumberLiteralNode.IntegerLiteralNode
): Constant<Any> {
    val value = expression.value
    return if (value.startsWith("-"))
        IntConstant(value.toLong(), location = expression.location)
    else
        UIntConstant(value.toULong(), location = expression.location)
}

val IdentifierNode.located: Located<String> get() = Located<String>(value, location)

fun resolveType(type: TypeNode?): Type {
    if (type == null) return Undefined
    return when (type) {
        is TypeNode.ArrayTypeNode -> Array(
            resolveType(type.type),
            type.fixedSize?.value?.toInt()
        )

        is TypeNode.FloatTypeNode -> Float(type.bitSize)
        is TypeNode.FunctionTypeSignatureNode -> TODO()
        is TypeNode.IntTypeNode -> Int(type.bitSize)
        is TypeNode.NameTypeNode -> TypeReference(type.name.located, type.genericTypes.map {
            Located(
                resolveType(it),
                it.location
            )
        })

        is TypeNode.StructuralInterfaceTypeNode -> StructuralInterface(type.fields.map {
            Field(
                it.name.located,
                false,
                resolveType(it.type)
            )
        }.associateBy { it.name.value })

        is TypeNode.UnionTypeNode -> Union(type.types.map { resolveType(it) })
        is TypeNode.ValueTypeNode -> TODO()
        is TypeNode.UIntTypeNode -> UInt(type.bitSize)
        is TypeNode.BooleanTypeNode -> Boolean
        is TypeNode.NullTypeNode -> TODO("Nullability not implemented yet")
    }
}
