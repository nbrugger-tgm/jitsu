package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.BiOperator
import eu.nitok.jitsu.compiler.ast.ExpressionNode
import eu.nitok.jitsu.compiler.ast.StatementNode
import eu.nitok.jitsu.compiler.ast.StatementNode.*
import eu.nitok.jitsu.compiler.ast.TypeNode

fun buildGraph(file: List<StatementNode>): Scope {
    val rootScope = Scope(null)//top level scopes have no parent
    for (statement in file) {
        when (statement) {
            is IfNode,
            is ReturnNode,
            is MethodInvocationNode,
            is FunctionCallNode,
            is AssignmentNode,
            is SwitchNode -> statement.errouneous("Statement not allowed at root level", rootScope)

            is CodeBlockNode -> statement.errouneous("Code block not allowed at root level", rootScope)
//            is EnumDeclarationNode -> TODO()
            is FunctionDeclarationNode -> {
                if (statement.name != null) buildFunctionGraph(statement, rootScope)
                else statement.errouneous("root level function must have a name", rootScope)
            }

            is TypeDefinitionNode -> TODO()
            is VariableDeclarationNode -> TODO()
        }
    }
    return rootScope
}

fun buildFunctionGraph(functionNode: FunctionDeclarationNode, parentScope: Scope) {
    val bodyScope = Scope(parentScope);
    val function = Function(bodyScope, functionNode.name)
    function.parameters.addAll(
        functionNode.parameters.map {
            val type = resolveType(it.type)
            Parameter(
                function, it.name, type,
                it.defaultValue?.let { it1 -> resolveConstant(parentScope, it1, type) }
            )
        }
    )
    val functionBody = mutableListOf<Instruction>()
}

fun resolveConstant(scope: Scope, expression: ExpressionNode, explicitType: ResolvedType?): Constant<Any>? {
    return when (expression) {
        is ExpressionNode.BooleanLiteralNode -> Constant.BooleanConstant(expression.value)
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> TODO()
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> resolveIntConstant(explicitType, expression, scope)
        is ExpressionNode.OperationNode -> resolveConstantOperation(scope, expression)
        is ExpressionNode.StatementExpressionNode -> TODO()
        is ExpressionNode.StringLiteralNode -> TODO()
        is ExpressionNode.VariableLiteralNode -> TODO()
        is ExpressionNode.FieldAccessNode -> TODO()
        is ExpressionNode.IndexAccessNode -> TODO()
    }
}

fun resolveConstantOperation(scope: Scope, expression: ExpressionNode.OperationNode): Constant<Any>? {
    val left = resolveConstant(scope, expression.left, null)
    val right = resolveConstant(scope, expression.right, null)
    if (left == null || right == null) return null;
    val operator = expression.operator
    return when (operator) {
        BiOperator.ADDITION -> when (left) {
            is Constant.StringConstant -> Constant.StringConstant(left.value + right.value.toString())
            is Constant.BooleanConstant -> if (right is Constant.StringConstant) Constant.StringConstant(left.value.toString() + right.value) else {
                scope.errors.add(Scope.Error("Cannot add boolean to ${right::class.simpleName}", expression.location))
                null
            }

            is Constant.IntConstant -> when (right) {
                is Constant.IntConstant -> Constant.IntConstant(left.value + right.value)
                is Constant.UIntConstant -> Constant.IntConstant(right.value.toLong() + left.value)

                is Constant.StringConstant -> Constant.StringConstant(left.value.toString() + right.value)
                else -> {
                    scope.errors.add(
                        Scope.Error(
                            "Cannot add integer to ${right::class.simpleName}",
                            expression.location
                        )
                    )
                    null
                }
            }

            is Constant.UIntConstant -> when (right) {
                is Constant.IntConstant -> Constant.IntConstant(left.value.toLong() + right.value)
                is Constant.UIntConstant -> Constant.UIntConstant(right.value + left.value)

                is Constant.StringConstant -> Constant.StringConstant(left.value.toString() + right.value)
                else -> {
                    scope.errors.add(
                        Scope.Error(
                            "Cannot add integer to ${right::class.simpleName}",
                            expression.location
                        )
                    )
                    null
                }
            }
        }

        BiOperator.SUBTRACTION, BiOperator.MODULO, BiOperator.MULTIPLICATION, BiOperator.DIVISION -> {
            val leftInt = when (left) {
                is Constant.IntConstant -> left.value.toBigInteger()
                is Constant.UIntConstant -> left.value.toLong().toBigInteger()
                else -> {
                    scope.errors.add(
                        Scope.Error("Cannot use math on ${left::class.simpleName}", expression.location)
                    )
                    return null;
                }
            };
            val rightInt = when (right) {
                is Constant.IntConstant -> right.value.toBigInteger()
                is Constant.UIntConstant -> right.value.toLong().toBigInteger()
                else -> {
                    scope.errors.add(
                        Scope.Error("Cannot use math on ${right::class.simpleName}", expression.location)
                    )
                    return null;
                }
            };
            return when (operator) {
                BiOperator.SUBTRACTION -> Constant.IntConstant((leftInt - rightInt).toLong())
                BiOperator.MODULO -> Constant.IntConstant((leftInt % rightInt).toLong())
                BiOperator.MULTIPLICATION -> Constant.IntConstant((leftInt * rightInt).toLong())
                BiOperator.DIVISION -> Constant.IntConstant((leftInt / rightInt).toLong())
                else -> throw IllegalStateException("Unreachable")
            }
        }

        BiOperator.GREATER, BiOperator.LESS, BiOperator.GREATER_EQUAL, BiOperator.LESS_EQUAL -> {
            val leftInt = when (left) {
                is Constant.IntConstant -> left.value.toBigInteger()
                is Constant.UIntConstant -> left.value.toLong().toBigInteger()
                else -> {
                    scope.errors.add(
                        Scope.Error("Cannot use math on ${left::class.simpleName}", expression.location)
                    )
                    return null;
                }
            };
            val rightInt = when (right) {
                is Constant.IntConstant -> right.value.toBigInteger()
                is Constant.UIntConstant -> right.value.toLong().toBigInteger()
                else -> {
                    scope.errors.add(
                        Scope.Error("Cannot use math on ${right::class.simpleName}", expression.location)
                    )
                    return null;
                }
            };
            return Constant.BooleanConstant(
                when (operator) {
                    BiOperator.GREATER -> leftInt > rightInt
                    BiOperator.LESS -> leftInt < rightInt
                    BiOperator.GREATER_EQUAL -> leftInt >= rightInt
                    BiOperator.LESS_EQUAL -> leftInt <= rightInt
                    else -> throw IllegalStateException("Unreachable")
                }
            )
        }

        BiOperator.AND -> if (left is Constant.BooleanConstant && right is Constant.BooleanConstant) {
            Constant.BooleanConstant(left.value && right.value)
        } else {
            scope.errors.add(
                Scope.Error(
                    "Cannot use AND on ${left::class.simpleName} and ${right::class.simpleName}",
                    expression.location
                )
            )
            null
        }

        BiOperator.OR -> if (left is Constant.BooleanConstant && right is Constant.BooleanConstant) {
            Constant.BooleanConstant(left.value || right.value)
        } else {
            scope.errors.add(
                Scope.Error(
                    "Cannot use OR on ${left::class.simpleName} and ${right::class.simpleName}",
                    expression.location
                )
            )
            null
        }
    }
}

private fun resolveIntConstant(
    explicitType: ResolvedType?,
    expression: ExpressionNode.NumberLiteralNode.IntegerLiteralNode,
    scope: Scope
): Constant<Any>? {
    explicitType?.let {
        when (it) {
            is ResolvedType.Int -> return Constant.IntConstant(expression.value.toLong(), it)
            is ResolvedType.UInt -> return Constant.UIntConstant(expression.value.toULong(), it)
            else -> {
                scope.errors.add(
                    Scope.Error(
                        "Cannot assign integer to ${explicitType::class.simpleName}",
                        expression.location
                    )
                )
                return null;
            }
        }
    }
    val value = expression.value
    return if (value.startsWith("-"))
        Constant.IntConstant(value.toLong())
    else
        Constant.UIntConstant(value.toULong())
}

fun resolveType(type: TypeNode): ResolvedType {
    TODO("Not yet implemented")
}

private fun StatementNode.errouneous(s: String, rootScope: Scope) {
    rootScope.errors.add(Scope.Error(s, this.location))
}
