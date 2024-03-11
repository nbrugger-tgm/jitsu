package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.*


import eu.nitok.jitsu.compiler.ast.StatementNode.*
import eu.nitok.jitsu.compiler.ast.StatementNode.Declaration.FunctionDeclarationNode
import eu.nitok.jitsu.compiler.ast.StatementNode.InstructionNode.*
import eu.nitok.jitsu.compiler.graph.Instruction.VariableDeclaration
import eu.nitok.jitsu.compiler.model.sequence

private class GraphBuilder {
    val messages: CompilerMessages = CompilerMessages()
}

fun buildGraph(srcFile: SourceFileNode): JitsuFile {
    return  GraphBuilder().buildGraph(srcFile);
}

private fun GraphBuilder.buildGraph(srcFile: SourceFileNode): JitsuFile {
    val file = JitsuFile(processStatements(srcFile.statements) {
        if(it !is Declaration) messages.error("Statement not allowed at root level", it.location)
    }, messages)
    file.informChildren()
    file.sequence().forEach {
        if(it is Access<*>) it.resolve(messages)
    }
    return file;
}

private fun GraphBuilder.processStatements(statements: List<StatementNode>, instructionHandler: (InstructionNode)->Unit): Scope {
    val functions: MutableList<Function> = mutableListOf()
    val variables: MutableList<Variable> = mutableListOf()
    val constants: MutableList<Constant<Any>> = mutableListOf()
    val types: MutableList<TypeDefinition> = mutableListOf()
    for (statement in statements) {
        when (statement) {
            is NamedTypeDeclarationNode.EnumDeclarationNode -> types.add(buildGraph(statement))
            is NamedTypeDeclarationNode.TypeAliasNode -> types.add(buildGraph(statement))
            is NamedTypeDeclarationNode.InterfaceTypeNode -> types.add(buildGraph(statement))
            is FunctionDeclarationNode -> {
                functions.add(buildFunctionGraph(statement))
                instructionHandler(statement)
            }
            is VariableDeclarationNode -> {
                val declaration = buildGraph(statement)
                instructionHandler(statement)
                variables.add(declaration.variable)
            }
            is InstructionNode -> instructionHandler(statement)
        }
    }
    return Scope(constants, types, functions, variables, messages)
}

private fun GraphBuilder.buildInstructionGraph(statement: InstructionNode): Instruction? {
    return when (statement) {
        is IfNode,
        is MethodInvocationNode,
        is AssignmentNode,
        is CodeBlockNode,
        is YieldStatement,
        is SwitchNode -> TODO()
        is FunctionCallNode -> Instruction.FunctionCall(statement.function.located, statement.parameters.map { buildExpressionGraph(it) })
        is ReturnNode -> Instruction.Return(buildExpressionGraph(statement.expression))
        is VariableDeclarationNode -> buildGraph(statement)
        is LineCommentNode -> null;
        is FunctionDeclarationNode -> buildFunctionGraph(statement)
    }
}

fun buildGraph(statement: VariableDeclarationNode): VariableDeclaration {
    val explicitType = statement.type?.let { resolveType(it) }
    val initialValue = buildExpressionGraph(statement.value)
    val variable = Variable(
        false,
        statement.name?.located ?: Located("unnamed", statement.keywordLocation),
        explicitType
    )
    return VariableDeclaration(
        variable,
        initialValue
    )
}

fun buildGraph(statement: NamedTypeDeclarationNode.TypeAliasNode): TypeDefinition.Alias {
    return TypeDefinition.Alias(statement.name.located, listOf(), lazy { resolveType(statement.type) })
}

private fun buildGraph(
    it: NamedTypeDeclarationNode.InterfaceTypeNode
) = TypeDefinition.Interface(
    it.name.located,
    listOf(),
    it.functions.associateBy(
        { func -> func.name.value },
        { func -> Located(buildGraph(func.typeSignature), func.typeSignature.location) }
    )
)

private fun buildGraph(
    it: TypeNode.FunctionTypeSignatureNode
) = Type.FunctionTypeSignature(
    it.returnType?.let { resolveType(it) },
    it.parameters.map {
        val type = resolveType(it.type);
        Type.FunctionTypeSignature.Parameter(it.name.located, type)
    }
)

private fun buildGraph(enum: NamedTypeDeclarationNode.EnumDeclarationNode): TypeDefinition.Enum {
    return TypeDefinition.Enum(enum.name.located, enum.constants.map { it.located })
}

private fun buildGraph(
    array: TypeNode.ArrayTypeNode
) = Type.Array(
    resolveType(array.type),
    array.fixedSize?.run { buildExpressionGraph(this) }
)

fun buildExpressionGraph(expression: ExpressionNode?): Expression {
    return when (expression) {
        null -> Expression.Undefined
        is ExpressionNode.BooleanLiteralNode -> Constant.BooleanConstant(expression.value, expression.location)
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> TODO()
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> resolveIntConstant(expression)
        is ExpressionNode.OperationNode -> Expression.Operation(
            buildExpressionGraph(expression.left),
            expression.operator.value,
            buildExpressionGraph(expression.right)
        );
        is ExpressionNode.StringLiteralNode -> TODO()
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
        expression.parameters.map { buildExpressionGraph(it) }
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
            buildExpressionGraph(it.defaultValue)
        )
    }
    val functionBody = when (val body = functionNode.body) {
        is CodeBlockNode.SingleExpressionCodeBlock -> TODO()//buildExpressionGraph(body.expression, function.bodyScope)
        is CodeBlockNode.StatementsCodeBlock -> buildCodeBlockGraph(body.statements)
        null -> CodeBlock(listOf(), Scope(mutableListOf(),mutableListOf(),mutableListOf(),mutableListOf(), messages));
    }
    return Function(
        name?.located,
        resolveType(functionNode.returnType),
        parameters,
        functionBody,
        messages
    );
}

private fun GraphBuilder.buildCodeBlockGraph(statements: List<StatementNode>): CodeBlock {
    val instructions = mutableListOf<Instruction>()
    val scope = processStatements(statements) {
        val instruction = buildInstructionGraph(it)
        if (instruction != null) instructions.add(instruction)
    }
    return CodeBlock(instructions, scope)
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
        Constant.IntConstant(value.toLong(), originLocation = expression.location)
    else
        Constant.UIntConstant(value.toULong(), originLocation = expression.location)
}

val IdentifierNode.located: Located<String> get() = Located(value, location)

fun resolveType(type: TypeNode?): Type {
    if (type == null) return Type.Undefined
    return when (type) {
        is TypeNode.ArrayTypeNode -> TODO()
        is TypeNode.FloatTypeNode -> TODO()
        is TypeNode.FunctionTypeSignatureNode -> TODO()
        is TypeNode.IntTypeNode -> Type.Int(type.bitSize)
        is TypeNode.NameTypeNode -> Type.TypeReference(type.name.located, mapOf())
        is TypeNode.StructuralInterfaceTypeNode -> TODO()
        is TypeNode.UnionTypeNode -> TODO()
        is TypeNode.ValueTypeNode -> TODO()
        is TypeNode.VoidTypeNode -> TODO()
        is TypeNode.UIntTypeNode -> Type.UInt(type.bitSize)
    }
}
