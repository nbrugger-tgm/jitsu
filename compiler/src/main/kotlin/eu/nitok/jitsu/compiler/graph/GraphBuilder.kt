package eu.nitok.jitsu.compiler.graph


import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Position
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.analysis.AnalysisRepository
import eu.nitok.jitsu.compiler.graph.ReferenceResolutionMode.RESOLVE
import eu.nitok.jitsu.compiler.graph.ReferenceResolutionMode.RESTORE
import eu.nitok.jitsu.compiler.graph.behaviour.Finalizable
import eu.nitok.jitsu.compiler.graph.behaviour.Resolvable
import eu.nitok.jitsu.compiler.graph.behaviour.Restorable
import eu.nitok.jitsu.compiler.graph.elements.*
import eu.nitok.jitsu.compiler.graph.elements.types.*
import eu.nitok.jitsu.compiler.graph.elements.types.Array
import eu.nitok.jitsu.compiler.graph.elements.types.Boolean
import eu.nitok.jitsu.compiler.graph.elements.types.Enum
import eu.nitok.jitsu.compiler.graph.elements.types.Float
import eu.nitok.jitsu.compiler.graph.elements.types.Int
import eu.nitok.jitsu.compiler.graph.elements.types.Struct.Field
import eu.nitok.jitsu.compiler.merge
import eu.nitok.jitsu.parser.ast.*
import eu.nitok.jitsu.parser.ast.StatementNode.*
import eu.nitok.jitsu.parser.ast.StatementNode.Declaration.FunctionDeclarationNode
import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.*
import java.net.URI

private class GraphBuilder {

    val messages: CompilerMessages = CompilerMessages()
}

internal data class ModuleCompilationResult(
    val module: JitsuModule,
    val messages: CompilerMessages
)

/**
 * Restores a de-serialized Module to its former interconnected state
 */
internal fun restoreJitsuModule(module: JitsuModule, dependencies: Map<String, JitsuModule> = mapOf()): CompilerMessages {
    val messages = CompilerMessages()
    val moduleLookup = merge(dependencies, module.moduleLookup) { a, b ->
        if (!(a.files.isEmpty() && b.files.isEmpty()))
            messages.error(
                "Module name conflict, ${a.fullyQualifiedName}, ${b.fullyQualifiedName}",
                Position(1, 1, (a.files.getOrNull(0) ?: b.files[0]).uri)
            )
        a
    }
    populateReferences(module, messages, moduleLookup, resolutionMode = RESTORE)
    return messages
}

internal fun buildJitsuModule(
    moduleAst: JitsuModuleAst,
    dependencies: Map<String, JitsuModule> = mapOf()
): ModuleCompilationResult {
    return GraphBuilder().buildGraph(moduleAst, dependencies)
}

internal fun buildJitsuModule(file: SourceFileNode): ModuleCompilationResult {
    return GraphBuilder().buildGraph(
        JitsuModuleAst(singleFileModuleName(file.url), listOf(file), listOf()),
        mapOf()
    )
}

private fun singleFileModuleName(file: URI): String {
    val parts = file.path.split("/")
    return parts.last().replace(".jit", "")
}

private fun GraphBuilder.buildGraph(moduleAst: JitsuModuleAst, dependencies: Map<String, JitsuModule>): ModuleCompilationResult {
    val messages = CompilerMessages()

    fun buildModule(
        prefix: String?,
        module: JitsuModuleAst
    ): JitsuModule {
        val moduleName = prefix?.let { "$it.${module.name}" } ?: module.name
        val subModules = module.modules.map { buildModule(moduleName, it) }
        val files = module.files.map { buildGraph(it, module.name) }
        val module = JitsuModule(moduleName, files, subModules)
        return module
    }

    val module = buildModule(null, moduleAst)

    val moduleLookup = merge(dependencies, module.moduleLookup) { a, b ->
        if (!(a.files.isEmpty() && b.files.isEmpty()))
            messages.error(
                "Module name conflict, ${a.fullyQualifiedName}, ${b.fullyQualifiedName}",
                Position(1, 1, URI((a.files.getOrNull(0) ?: b.files[0]).path))
            )
        a
    }

    //backreferences
    populateReferences(module, messages, moduleLookup)

    val repository = AnalysisRepository()
    val allFiles = module.allModules.flatMap { it.files }.toList()
    val topLevelFunctions = allFiles.flatMap { it.functions }
    repository.analyzeAll(topLevelFunctions, messages)
    allFiles.asSequence().flatMap { it.sequence() }.forEach {
        if (it is FunctionElement) {
            it.summary = repository.getFunctionSummary(it)
        }
    }
    return ModuleCompilationResult(module, messages)
}

private enum class ReferenceResolutionMode {
    RESOLVE, RESTORE
}
private fun populateReferences(
    module: JitsuModule,
    messages: CompilerMessages,
    moduleLookup: Map<String, JitsuModule>,
    resolutionMode: ReferenceResolutionMode = RESOLVE
) {
    module.setScopes()
    module.sequence().forEach {
        if (it is Import) {
            it.resolve(messages, moduleLookup)
        }
        if(it is HasAttributesElement) it.attributes.forEach { attr ->
            attr.attachTo(it)
        }
    }
    module.sequence().forEach(
        when(resolutionMode) {
            RESOLVE -> ({ if (it is Resolvable) it.resolve(messages) })
            RESTORE -> ({ if (it is Restorable) it.restore(messages) })
        }
    )
    module.sequence().forEach {
        if (it is Finalizable) it.finalize(messages)
    }
}

private fun GraphBuilder.buildGraph(
    srcFile: SourceFileNode, module: String,
): JitsuFile {
    val statements = processStatements(srcFile.statements, module) {
        if (it !is Declaration) messages.error("Statement not allowed at root level", it.location)
    }
    return JitsuFile(
        functions = statements.functions,
        typeElements = statements.types,
        variableElements = statements.variables,
        imports = statements.imports,
        attributes = statements.attributes,
        path = srcFile.url.toString(), //TODO: toString should not be needed
    )
}

private data class Statements(
    val functions: List<FunctionElement>,
    val variables: List<VariableDeclaration>,
    val constants: List<ConstantElement<Any>>,
    val types: List<TypeDefinitionElement>,
    val attributes: List<AttributeDefinitionElement>,
    val imports: List<Import>
)

private fun GraphBuilder.processStatements(
    statements: List<StatementNode>,
    module: String?,
    instructionHandler: (InstructionNode) -> Unit
): Statements {
    val functions: MutableList<FunctionElement> = mutableListOf()
    val variables: MutableList<VariableDeclaration> = mutableListOf()
    val constants: MutableList<ConstantElement<Any>> = mutableListOf()
    val types: MutableList<TypeDefinitionElement> = mutableListOf()
    val attributes: MutableList<AttributeDefinitionElement> = mutableListOf()
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

            is AttributeDeclarationNode -> buildGraph(statement)?.let { attributes.add(it) }
        }
    }
    return Statements(functions, variables, constants, types, attributes, imports.values.toList())
}

private fun GraphBuilder.buildGraph(attributeDefinitionElement: AttributeDeclarationNode): AttributeDefinitionElement? {
    val name = attributeDefinitionElement.name ?: return null
    return AttributeDefinitionElement(
        name.located,
        attributeDefinitionElement.properties.mapNotNull { property ->
            val propName = property.name ?: return@mapNotNull null
            AttributeDefinitionElement.Property(
                propName.located,
                property.type?.let { rawType(it) } ?: Undefined
            )
        }
    )

}

private fun GraphBuilder.buildClassGraph(classNode: NamedTypeDeclarationNode.ClassDeclarationNode): Class? {
    return classNode.name?.let { name ->
        val fields = classNode.fields.map { field ->
            Field(
                field.name.located,
                field.mutableKw != null,
                rawType(field.type)
            )
        }
        return Class(
            name.located,
            classNode.typeParameters.map { buildTypeParameterGraph(it) },
            fields,
            classNode.methods.map {
                val base = buildFunctionGraph(it.function)
                FunctionElement(
                    base.name,
                    base.returnTypeElement,
                    listOf(
                        FunctionElement.Parameter(
                            Located("this", name.location),
                            TypeReference(name.located, listOf()),
                            null
                        )
                    ) + base.parameters,
                    base.bodyElement,
                    it.function.attributes.mapNotNull { buildAttributeUseGraph(it) },
                    base.location
                )
            }
        )
    }
}

private fun GraphBuilder.buildInstructionGraph(statement: InstructionNode): InstructionElement? {
    return when (statement) {
        is IfNode,
        is MethodInvocationNode,
        is AssignmentNode,
        is CodeBlockNode,
        is YieldStatement,
        is SwitchNode -> TODO()


        is FunctionCallNode -> FunctionCall(
            statement.function.located,
            statement.parameters.map { buildExpressionGraph(it) },
            statement.location
        )

        is ReturnNode -> Return(
            statement.expression?.let { node -> buildExpressionGraph(node) },
            statement.keywordLocation
        )

        is VariableDeclarationNode -> buildGraph(statement)
        is LineCommentNode -> null
        is FunctionDeclarationNode -> buildFunctionGraph(statement)
    }
}

private fun GraphBuilder.buildGraph(statement: VariableDeclarationNode): VariableDeclaration {
    val explicitType = statement.type?.let { rawType(it) }
    val initialValue = statement.value?.let { node -> buildExpressionGraph(node) }
    val variableDeclaration = VariableDeclaration(
        false,
        statement.name?.located ?: Located("unnamed", statement.keywordLocation),
        explicitType,
        initialValue
    )
    return variableDeclaration
}

private fun GraphBuilder.buildGraph(statement: NamedTypeDeclarationNode.TypeAliasNode): TypeAlias? {
    return statement.name?.let {
        TypeAlias(
            it.located,
            statement.typeParameters.map { buildTypeParameterGraph(it) },
            rawType(statement.type)
        )
    }
}

private fun GraphBuilder.buildGraph(
    it: NamedTypeDeclarationNode.InterfaceTypeNode
) = Interface(
    it.name.located,
    listOf(),
    it.functions.map { func -> NamedFunctionSignature(func.name.located, buildGraph(func.typeSignature)) }
)

private fun GraphBuilder.buildGraph(
    it: TypeNode.FunctionTypeSignatureNode
) = FunctionTypeSignature(
    it.returnType?.let { rawType(it) },
    it.parameters.map {
        val type = rawType(it.type)
        FunctionTypeSignature.Parameter(it.name.located, type, false)
    }
)

private fun buildGraph(enum: NamedTypeDeclarationNode.EnumDeclarationNode): Enum {
    return Enum(
        enum.name.located,
        enum.constants.map { Enum.Constant(it.located) })
}

private fun buildTypeParameterGraph(node: IdentifierNode): TypeParameterElement = TypeParameterElement(node.located)

private fun buildExpressionGraph(expression: ExpressionNode): ExpressionElement {
    return when (expression) {
        is ExpressionNode.BooleanLiteralNode -> ConstantElement.BooleanConstant(expression.value, expression.location)
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> TODO()
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> resolveIntConstant(expression)
        is ExpressionNode.OperationNode -> {
            val left = buildExpressionGraph(expression.left)
            val right = expression.right?.let { buildExpressionGraph(it) }
                ?: UndefinedExpression(expression.operator.location)
            val operator = expression.operator
            FunctionCall(
                operator.map { it.functionName },
                listOf(left, right),
                left.location.rangeTo(right.location),
            )
        }

        is ExpressionNode.StringLiteralNode ->
            //TODO reflect propper string literals in IR
            ConstantElement.StringConstant(expression.toString(), expression.location)
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

private fun resolveFunctionCall(expression: FunctionCallNode): FunctionCall {
    return FunctionCall(
        expression.function.located,
        expression.parameters.map { buildExpressionGraph(it) },
        expression.location
    )
}

private fun resolveVariableReference(
    expression: ExpressionNode.VariableReferenceNode
): ExpressionElement {
    return VariableReference(expression.variable.located)
}

private fun GraphBuilder.buildFunctionGraph(functionNode: FunctionDeclarationNode): FunctionElement {
    val name = functionNode.name
    val parameters = functionNode.parameters.map {
        val type = rawType(it.type)
        FunctionElement.Parameter(
            it.name.located, type,
            it.defaultValue?.let { buildExpressionGraph(it) }
        )
    }
    val attributes = functionNode.attributes.mapNotNull { buildAttributeUseGraph(it) }
    val functionBody = when (val body = functionNode.body) {
        is CodeBlockNode.SingleExpressionCodeBlock -> TODO()//buildExpressionGraph(body.expression, function.bodyScope)
        is CodeBlockNode.StatementsCodeBlock -> FunctionElement.BodyElement.Implementation(buildCodeBlockGraph(body.statements))
        is FunctionDeclarationNode.FunctionBodyNode.NativeImplementation -> FunctionElement.BodyElement.Native

        null -> FunctionElement.BodyElement.Missing
    }
    return FunctionElement(
        name?.located,
        functionNode.returnType?.let { Located(rawType(it), it.location) },
        parameters,
        functionBody,
        attributes,
        functionNode.name?.location ?: functionNode.keywordLocation
    )
}

private fun GraphBuilder.buildCodeBlockGraph(statements: List<StatementNode>): CodeBlockElement {
    val instructions = mutableListOf<InstructionElement>()
    processStatements(statements, null) {
        val instruction = buildInstructionGraph(it)
        if (instruction != null) instructions.add(instruction)
    }
    return CodeBlockElement(instructions)
}

private fun resolveConstantOperation(scope: Scope, expression: ExpressionNode.OperationNode): ConstantElement<Any>? {
//    val left = resolveConstant(scope, expression.left, null)
//    val right = resolveConstant(scope, expression.right, null)
//    if (left == null || right == null) return null
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
//                    return null
//                }
//            }
//            val rightInt = when (right) {
//                is Constant.IntConstant -> right.value.toBigInteger()
//                is Constant.UIntConstant -> right.value.toLong().toBigInteger()
//                else -> {
//                    scope.errors.add(
//                        Scope.Error("Cannot use math on ${right::class.simpleName}", expression.location)
//                    )
//                    return null
//                }
//            }
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
//                    return null
//                }
//            }
//            val rightInt = when (right) {
//                is Constant.IntConstant -> right.value.toBigInteger()
//                is Constant.UIntConstant -> right.value.toLong().toBigInteger()
//                else -> {
//                    scope.errors.add(
//                        Scope.Error("Cannot use math on ${right::class.simpleName}", expression.location)
//                    )
//                    return null
//                }
//            }
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
    TODO()
}

private fun resolveIntConstant(
    expression: ExpressionNode.NumberLiteralNode.IntegerLiteralNode
): ConstantElement<Any> {
    val value = expression.value
    return if (value.startsWith("-"))
        ConstantElement.IntConstant(value.toLong(), location = expression.location)
    else
        ConstantElement.UIntConstant(value.toULong(), location = expression.location)
}

val IdentifierNode.located: Located<String> get() = Located(value, location)

private fun GraphBuilder.rawType(type: TypeNode?): TypeElement {
    if(type == null) return Undefined
    return when (type) {
        is TypeNode.ArrayTypeNode -> {
            val size = type.fixedSize?.let {
                resolveIntConstant(it) as? ConstantElement.UIntConstant ?: run {
                    messages.error("Array size must be positive", it)
                    null
                }
            }
            Array(rawType(type.type), size)
        }

        is TypeNode.FloatTypeNode -> Float(type.bitSize)
        is TypeNode.IntTypeNode -> Int(type.bitSize)
        is TypeNode.NameTypeNode -> TypeReference(type.name.located, type.genericTypes.map {
            Located(
                rawType(it),
                it.location
            )
        })

        is TypeNode.StructuralInterfaceTypeNode -> StructuralInterface(type.fields.map {
            Field(
                it.name.located,
                false,
                rawType(it.type)
            )
        }.associateBy { it.name.value })

        is TypeNode.UnionTypeNode -> Union(type.types.map { rawType(it) })
        is TypeNode.UIntTypeNode -> UInt(type.bitSize)
        is TypeNode.BooleanTypeNode -> Boolean
        is TypeNode.FunctionTypeSignatureNode -> TODO()
        is TypeNode.ValueTypeNode -> TODO("Needs constant expression resolution")
        is TypeNode.NullTypeNode -> Null
    }
}

/**
 * @return null if the attribute has no name
 */
private fun buildAttributeUseGraph(attribute: AttributeNode): AttributeElement? {
    val name = attribute.name?.located ?: return null
    val props = attribute.values.map { property ->
        AttributeElement.Property(property.name.located,property.value?.let { expression ->  buildExpressionGraph(expression) }?: UndefinedExpression(property.name.location))
    }
    return AttributeElement(
        name,
        props
    )
}