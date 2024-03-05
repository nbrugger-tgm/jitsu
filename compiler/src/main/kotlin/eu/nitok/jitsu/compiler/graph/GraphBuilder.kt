package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.*


import eu.nitok.jitsu.compiler.ast.StatementNode.*

fun buildGraph(file: SourceFileNode): Scope {
    val rootScope = Scope(null)//top level scopes have no parent
    for (statement in file.statements) {
        when (statement) {
            is IfNode,
            is ReturnNode,
            is MethodInvocationNode,
            is FunctionCallNode,
            is AssignmentNode,
            is SwitchNode -> rootScope.error("Statement not allowed at root level", statement.location)
            is CodeBlockNode -> rootScope.error("Code block not allowed at root level", statement.location)
            is NamedTypeDeclarationNode.EnumDeclarationNode -> buildGraph(statement)
            is NamedTypeDeclarationNode.TypeAliasNode -> buildGraph(statement, rootScope)
            is NamedTypeDeclarationNode.InterfaceTypeNode -> buildGraph(statement, rootScope)
            is VariableDeclarationNode -> TODO()
            is LineCommentNode -> TODO()
            is YieldStatement -> TODO()
            is FunctionDeclarationNode -> buildFunctionGraph(statement, rootScope)
        }
    }
    return rootScope
}

fun buildGraph(statement: NamedTypeDeclarationNode.TypeAliasNode, scope: Scope) {
    val alias = ResolvedType.NamedType.Alias(statement.name.located, buildGraph(statement.type, scope))
    scope.register(alias);
}

fun buildGraph(it: TypeNode, scope: Scope): Lazy<ResolvedType> {
    return lazy {
        when (it) {
            is TypeNode.FloatTypeNode -> ResolvedType.Float(it.bitSize)
            is TypeNode.IntTypeNode -> ResolvedType.Int(it.bitSize)
            is TypeNode.ArrayTypeNode -> buildGraph(it, scope)//TODO: reduce multi dimension
            is TypeNode.FunctionTypeSignatureNode -> buildGraph(it, scope)
            is TypeNode.StringTypeNode -> TODO()
            is TypeNode.UnionTypeNode -> TODO()
            is TypeNode.ValueTypeNode -> TODO()
            is TypeNode.VoidTypeNode -> TODO()
            is TypeNode.StructuralInterfaceTypeNode -> TODO()
            is TypeNode.NameTypeNode -> scope.resolveType(it.name.located)
        }
    }
}

private fun buildGraph(
    it: NamedTypeDeclarationNode.InterfaceTypeNode,
    scope: Scope
) = ResolvedType.NamedType.Interface(
    it.name.located,
    it.functions.associateBy(
        { func -> func.name.value },
        { func -> Located(buildGraph(func.typeSignature, scope), func.typeSignature.location) }
    )
)

private fun buildGraph(
    it: TypeNode.FunctionTypeSignatureNode,
    scope: Scope
) = ResolvedType.FunctionTypeSignature(
    it.returnType?.run { buildGraph(this, scope) },
    it.parameters.map {
        it.run {
            val type = type?.run { buildGraph(this, scope) } ?: lazy { ResolvedType.Undefined };
            ResolvedType.FunctionTypeSignature.Parameter(name.located, type)
        }
    }
)

private fun buildGraph(enum: NamedTypeDeclarationNode.EnumDeclarationNode): ResolvedType.NamedType.Enum {
    return ResolvedType.NamedType.Enum(enum.name.located, enum.constants.map { it.located })
}

private fun buildGraph(
    it: TypeNode.ArrayTypeNode,
    scope: Scope
) =
    ResolvedType.Array(
        buildGraph(it.type, scope),
        it.fixedSize?.run { buildExpressionGraph(this, scope) }
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
    explicitType: ResolvedType?,
    expression: ExpressionNode.NumberLiteralNode.IntegerLiteralNode,
    scope: Scope
): Constant<Any>? {
    explicitType?.let {
        when (it) {
            is ResolvedType.Int -> return Constant.IntConstant(expression.value.toLong(), it, expression.location)
            is ResolvedType.UInt -> return Constant.UIntConstant(expression.value.toULong(), it, expression.location)
            else -> {
                scope.error("Cannot assign integer to $it", expression.location)
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

val IdentifierNode.located : Located<String> get() = Located(value, location)

fun resolveType(type: TypeNode): ResolvedType {
    TODO("Not yet implemented")
}
