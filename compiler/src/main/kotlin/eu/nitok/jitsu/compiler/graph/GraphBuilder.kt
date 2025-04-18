package eu.nitok.jitsu.compiler.graph


import eu.nitok.jitsu.compiler.ast.*
import eu.nitok.jitsu.compiler.ast.StatementNode.*
import eu.nitok.jitsu.compiler.ast.StatementNode.Declaration.FunctionDeclarationNode
import eu.nitok.jitsu.compiler.ast.StatementNode.InstructionNode.*
import eu.nitok.jitsu.compiler.graph.TypeDefinition.TypeParameter
import eu.nitok.jitsu.compiler.model.sequence
import eu.nitok.jitsu.compiler.model.walk

private class GraphBuilder {
    val messages: CompilerMessages = CompilerMessages()
}

fun buildGraph(srcFile: SourceFileNode): JitsuFile {
    return GraphBuilder().buildGraph(srcFile);
}

private fun GraphBuilder.buildGraph(srcFile: SourceFileNode): JitsuFile {
    val file = JitsuFile(processStatements(srcFile.statements) {
        if (it !is Declaration) messages.error("Statement not allowed at root level", it.location)
    }, messages)
    file.setScopes()
    file.walk {
        if (it is Access<*>) it.finalize(messages)

        //code blocks require sequential processing for functions & variable accesses
        if (it is CodeBlock) it.walk {
            if (it is Access.TypeAccess) it.finalize(messages)
            return@walk true;
        }
        return@walk it !is CodeBlock
    }
    file.sequence().forEach {
        if (it is Finalizable) it.finalizeGraph(messages)
    }
    return file;
}

private fun GraphBuilder.processStatements(
    statements: List<StatementNode>,
    instructionHandler: (InstructionNode) -> Unit
): Scope {
    val functions: MutableList<Function> = mutableListOf()
    val variables: MutableList<Variable> = mutableListOf()
    val constants: MutableList<Constant<Any>> = mutableListOf()
    val types: MutableList<TypeDefinition> = mutableListOf()
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
                val declaration = buildGraph(statement)
                instructionHandler(statement)
                variables.add(declaration)
            }

            is InstructionNode -> instructionHandler(statement)
            is NamedTypeDeclarationNode.ClassDeclarationNode -> buildClassGraph(statement)?.let { types.add(it) }
        }
    }
    return Scope(constants, types, functions, variables, messages)
}

private fun GraphBuilder.buildClassGraph(classNode: NamedTypeDeclarationNode.ClassDeclarationNode): TypeDefinition.ParameterizedType.Class? {
    return classNode.name?.let { name ->
        val fields = classNode.fields.map { field ->
            TypeDefinition.ParameterizedType.Struct.Field(
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
                            Located("this", name.location),
                            Type.TypeReference(name.located, listOf()),
                            null
                        )
                    ) + base.parameters,
                    base.body
                )
            })
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
        statement.name?.located ?: Located("unnamed", statement.keywordLocation),
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
) = Type.FunctionTypeSignature(
    it.returnType?.let { resolveType(it) },
    it.parameters.map {
        val type = resolveType(it.type);
        Type.FunctionTypeSignature.Parameter(it.name.located, type, false)
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
) = Type.Array(
    resolveType(array.type),
    array.fixedSize?.run { buildExpressionGraph(this) }
)

fun buildExpressionGraph(expression: ExpressionNode): Expression {
    return when (expression) {
        is ExpressionNode.BooleanLiteralNode -> Constant.BooleanConstant(expression.value, expression.location)
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> TODO()
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> resolveIntConstant(expression)
        is ExpressionNode.OperationNode -> Expression.Operation(
            buildExpressionGraph(expression.left),
            expression.operator.value,
            expression.right?.let { buildExpressionGraph(it) } ?: Expression.Undefined(expression.operator.location)
        );
        is ExpressionNode.StringLiteralNode -> Constant.StringConstant(expression.toString(), expression.location)
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
    return Expression.VariableReference(expression.variable.located)
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
        is CodeBlockNode.StatementsCodeBlock -> buildCodeBlockGraph(body.statements)
        null -> CodeBlock(listOf());
    }
    return Function(
        name?.located,
        resolveType(functionNode.returnType),
        parameters,
        functionBody
    );
}

private fun GraphBuilder.buildCodeBlockGraph(statements: List<StatementNode>): CodeBlock {
    val instructions = mutableListOf<Instruction>()
    processStatements(statements) {
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
        Constant.IntConstant(value.toLong(), location = expression.location)
    else
        Constant.UIntConstant(value.toULong(), location = expression.location)
}

val IdentifierNode.located: Located<String> get() = Located(value, location)

fun resolveType(type: TypeNode?): Type {
    if (type == null) return Type.Undefined
    return when (type) {
        is TypeNode.ArrayTypeNode -> Type.Array(
            resolveType(type.type),
            type.fixedSize?.let { buildExpressionGraph(it) })

        is TypeNode.FloatTypeNode -> Type.Float(type.bitSize)
        is TypeNode.FunctionTypeSignatureNode -> TODO()
        is TypeNode.IntTypeNode -> Type.Int(type.bitSize)
        is TypeNode.NameTypeNode -> Type.TypeReference(type.name.located, type.genericTypes.map { Located(resolveType(it), it.location) })
        is TypeNode.StructuralInterfaceTypeNode -> Type.StructuralInterface(type.fields.map {
            TypeDefinition.ParameterizedType.Struct.Field(
                it.name.located,
                false,
                resolveType(it.type)
            )
        }.associateBy { it.name.value })

        is TypeNode.UnionTypeNode -> Type.Union(type.types.map { resolveType(it) })
        is TypeNode.ValueTypeNode -> TODO()
        is TypeNode.VoidTypeNode -> TODO()
        is TypeNode.UIntTypeNode -> Type.UInt(type.bitSize)
    }
}
