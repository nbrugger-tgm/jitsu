package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.*


import eu.nitok.jitsu.compiler.ast.StatementNode.*
import eu.nitok.jitsu.compiler.model.BitSize

//
fun buildGraph(file: List<AstNode<StatementNode>>): Scope {
    val rootScope = Scope(null)//top level scopes have no parent
    for (statementNode in file) {
        if (statementNode is AstNode.Error)
            continue;
        when (val statement = (statementNode as AstNode.Node).value) {
            is IfNode,
            is ReturnNode,
            is MethodInvocationNode,
            is FunctionCallNode,
            is AssignmentNode,
            is SwitchNode -> rootScope.error("Statement not allowed at root level" to statement.location)

            is CodeBlockNode -> rootScope.error("Code block not allowed at root level" to statement.location)
            is TypeDefinitionNode -> buildGraph(statement, rootScope)
            is VariableDeclarationNode -> TODO()
            is TypeNode.InterfaceTypeNode -> TODO()
            is LineCommentNode -> TODO()
            is YieldStatement -> TODO()
            is FunctionDeclarationNode -> {
                if (statement.name != null) buildFunctionGraph(statement, rootScope)
                else rootScope.error("root level function must have a name" to statement.keywordLocation)
            }
        }
    }
    return rootScope
}

fun buildGraph(statement: TypeDefinitionNode, scope: Scope) {
    val name = statement.name
    if (name is AstNode.Node) {
        scope.register(name.value) {
            statement.type.map { buildGraph(it, scope) }
        }
    }
}

fun buildGraph(it: TypeNode, scope: Scope): ResolvedType {
    return when (it) {
        is TypeNode.ArrayTypeNode -> buildGraph(it, scope)//TODO: reduce multi dimension
        is TypeNode.EnumDeclarationNode -> buildGraph(it)
        is TypeNode.FloatTypeNode -> ResolvedType.Float(it.bitSize.orNull() ?: BitSize.BIT_32)
        is TypeNode.FunctionTypeSignatureNode -> buildGraph(it, scope)
        is TypeNode.IntTypeNode -> ResolvedType.Int(it.bitSize.orNull() ?: BitSize.BIT_32)
        is TypeNode.InterfaceTypeNode -> buildGraph(it, scope)
        is TypeNode.NamedTypeNode -> ResolvedType.Named(it.name)
        is TypeNode.StringTypeNode -> TODO()
        is TypeNode.UnionTypeNode -> TODO()
        is TypeNode.ValueTypeNode -> TODO()
        is TypeNode.VoidTypeNode -> TODO()
    }
}

private fun buildGraph(
    it: TypeNode.InterfaceTypeNode,
    scope: Scope
) = ResolvedType.Interface(it.functions.mapNotNull { it.orNull() }
    .mapNotNull { it.name.orNull()?.to(it.typeSignature) }
    .associateBy(
        { func -> func.first.value },
        { func -> buildGraph(func.second, scope) to func.first.location }
    )
)

private fun buildGraph(
    it: TypeNode.FunctionTypeSignatureNode,
    scope: Scope
) = ResolvedType.FunctionTypeSignature(
    it.returnType?.map { buildGraph(it, scope) }?.orNull(),
    it.parameters.mapNotNull {
        it.map {
            val name = it.name.orNull() ?: return@map null;
            val type = it.type.orNull() ?: return@map null;
            ResolvedType.FunctionTypeSignature.Parameter(name, buildGraph(type, scope))
        }.orNull()
    }
)

private fun buildGraph(enum: TypeNode.EnumDeclarationNode): ResolvedType.Enum {
    return ResolvedType.Enum(enum.constants.mapNotNull { node ->
        node.map { it.name to it.location }.orNull()
    })
}

private fun buildGraph(
    it: TypeNode.ArrayTypeNode,
    scope: Scope
) =
    ResolvedType.ComplexType.Array(
        it.type.map { buildGraph(it, scope) },
        it.fixedSize?.map { buildExpressionGraph(it, scope) }?.orNull()
    )

fun buildExpressionGraph(it: ExpressionNode, scope: Scope): Expression {
    TODO("Not yet implemented")
}

fun buildFunctionGraph(functionNode: FunctionDeclarationNode, parentScope: Scope) {
//    val bodyScope = Scope(parentScope);
//    val function = Function(bodyScope, functionNode.name.or)
//    function.parameters.addAll(
//        functionNode.parameters.map {
//            val type = resolveType(it.type)
//            Parameter(
//                function, it.name, type,
//                it.defaultValue?.let { it1 -> resolveConstant(parentScope, it1, type) }
//            )
//        }
//    )
//    val functionBody = mutableListOf<Instruction>()
    TODO();
}

//fun resolveConstant(scope: Scope, expression: ExpressionNode, explicitType: ResolvedType?): Constant<Any>? {
//    return when (expression) {
//        is ExpressionNode.BooleanLiteralNode -> Constant.BooleanConstant(expression.value)
//        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> TODO()
//        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> resolveIntConstant(explicitType, expression, scope)
//        is ExpressionNode.OperationNode -> resolveConstantOperation(scope, expression)
//        is ExpressionNode.StringLiteralNode -> TODO()
//        is ExpressionNode.VariableLiteralNode -> TODO()
//        is ExpressionNode.FieldAccessNode -> TODO()
//        is ExpressionNode.IndexAccessNode -> TODO()
//        is CodeBlockNode.SingleExpressionCodeBlock -> TODO()
//        is CodeBlockNode.StatementsCodeBlock -> TODO()
//        is FunctionCallNode -> TODO()
//        is FunctionDeclarationNode -> TODO()
//        is IfNode -> TODO()
//        is MethodInvocationNode -> TODO()
//        is SwitchNode -> TODO()
//    }
//}

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
    explicitType: DeclaredType?,
    expression: ExpressionNode.NumberLiteralNode.IntegerLiteralNode,
    scope: Scope
): Constant<Any>? {
    explicitType?.let {
        when (val type = it.value) {
            is ResolvedType.Int -> return Constant.IntConstant(expression.value.toLong(), type, expression.location)
            is ResolvedType.UInt -> return Constant.UIntConstant(expression.value.toULong(), type, expression.location)
            else -> {
                scope.error("Cannot assign integer to {}" to expression.location, it)
                return null;
            }
        }
    }
    val value = expression.value
    return if (value.startsWith("-"))
        Constant.IntConstant(value.toLong(), originLocation = expression.location)
    else
        Constant.UIntConstant(value.toULong(), originLocation = expression.location)
}

fun resolveType(type: TypeNode): ResolvedType {
    TODO("Not yet implemented")
}
