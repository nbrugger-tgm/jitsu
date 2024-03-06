package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.*


import eu.nitok.jitsu.compiler.ast.StatementNode.*

fun buildGraph(file: SourceFileNode): Scope {
    val rootScope = Scope()//top level scopes have no parent
    for (statement in file.statements) {
        when (statement) {
            is IfNode,
            is ReturnNode,
            is MethodInvocationNode,
            is FunctionCallNode,
            is AssignmentNode,
            is SwitchNode -> rootScope.error("Statement not allowed at root level", statement.location)

            is CodeBlockNode -> rootScope.error("Code block not allowed at root level", statement.location)
            is NamedTypeDeclarationNode.EnumDeclarationNode -> rootScope.register(buildGraph(rootScope, statement))
            is NamedTypeDeclarationNode.TypeAliasNode -> rootScope.register(buildGraph(rootScope, statement))
            is NamedTypeDeclarationNode.InterfaceTypeNode -> rootScope.register(buildGraph(rootScope, statement))
            is VariableDeclarationNode -> TODO()
            is LineCommentNode -> TODO()
            is YieldStatement -> TODO()
            is FunctionDeclarationNode -> rootScope.register(buildFunctionGraph(statement, rootScope))
        }
    }
    return rootScope
}

fun buildGraph(scope: Scope, statement: StatementNode): Instruction? {
    return when (statement) {
        is IfNode,
        is MethodInvocationNode,
        is FunctionCallNode,
        is AssignmentNode,
        is CodeBlockNode,
        is YieldStatement,
        is SwitchNode -> TODO()

        is ReturnNode -> Instruction.Return(buildExpressionGraph(statement.expression, scope))

        is VariableDeclarationNode -> buildGraph(scope, statement)

        is NamedTypeDeclarationNode.EnumDeclarationNode -> {
            scope.register(buildGraph(scope, statement))
            null
        }

        is NamedTypeDeclarationNode.TypeAliasNode -> {
            scope.register(buildGraph(scope, statement))
            null
        }

        is NamedTypeDeclarationNode.InterfaceTypeNode -> {
            scope.register(buildGraph(scope, statement))
            null
        }

        is FunctionDeclarationNode -> {
            scope.register(buildFunctionGraph(statement, scope))
            null
        }

        is LineCommentNode -> null;
    }
}

fun buildGraph(scope: Scope, statement: VariableDeclarationNode): Instruction {
    val explicitType = resolveType(scope, statement.type)
    val initialValue = buildExpressionGraph(statement.value, scope)
    return Instruction.VariableDeclaration(
        Variable(
            false,
            statement.name?.located ?: Located("unnamed", statement.keywordLocation),
            explicitType,
            explicitType
        ),
        initialValue
    )
}

fun buildGraph(scope: Scope, statement: NamedTypeDeclarationNode.TypeAliasNode): TypeDefinition.Alias {
    return TypeDefinition.Alias(statement.name.located, listOf(), lazy { resolveType(scope, statement.type) })
}

private fun buildGraph(
    scope: Scope,
    it: NamedTypeDeclarationNode.InterfaceTypeNode
) = TypeDefinition.Interface(
    it.name.located,
    listOf(),
    it.functions.associateBy(
        { func -> func.name.value },
        { func -> Located(buildGraph(func.typeSignature, scope), func.typeSignature.location) }
    )
)

private fun buildGraph(
    it: TypeNode.FunctionTypeSignatureNode,
    scope: Scope
) = Type.FunctionTypeSignature(
    it.returnType?.let { resolveType(scope, it) },
    it.parameters.map {
        val type = resolveType(scope, it.type);
        Type.FunctionTypeSignature.Parameter(it.name.located, type)
    }
)

private fun buildGraph(scope: Scope, enum: NamedTypeDeclarationNode.EnumDeclarationNode): TypeDefinition.Enum {
    return TypeDefinition.Enum(enum.name.located, enum.constants.map { it.located })
}

private fun buildGraph(
    scope: Scope,
    array: TypeNode.ArrayTypeNode
) = Type.Array(
    resolveType(scope, array.type),
    array.fixedSize?.run { buildExpressionGraph(this, scope) }
)

fun buildExpressionGraph(expression: ExpressionNode?, scope: Scope): Expression {
    return when (expression) {
        null -> Expression.Undefined
        is ExpressionNode.BooleanLiteralNode -> Constant.BooleanConstant(expression.value, expression.location)
        is ExpressionNode.NumberLiteralNode.FloatLiteralNode -> TODO()
        is ExpressionNode.NumberLiteralNode.IntegerLiteralNode -> resolveIntConstant(expression)
        is ExpressionNode.OperationNode -> Expression.Operation(
            buildExpressionGraph(expression.left, scope),
            expression.operator.value,
            buildExpressionGraph(expression.right, scope)
        );
        is ExpressionNode.StringLiteralNode -> TODO()
        is ExpressionNode.VariableReferenceNode -> TODO()
        is ExpressionNode.FieldAccessNode -> TODO()
        is ExpressionNode.IndexAccessNode -> TODO()
        is CodeBlockNode.SingleExpressionCodeBlock -> TODO()
        is CodeBlockNode.StatementsCodeBlock -> TODO()
        is FunctionCallNode -> TODO()
        is FunctionDeclarationNode -> TODO()
        is IfNode -> TODO()
        is MethodInvocationNode -> TODO()
        is SwitchNode -> TODO()
    }
}

fun resolveVariableReference(expression: ExpressionNode.VariableReferenceNode): Expression {

}

fun buildFunctionGraph(functionNode: FunctionDeclarationNode, parentScope: Scope): Function {
    val name = functionNode.name
    val innerScope = Scope(parentScope);
    val parameters = functionNode.parameters.map {
        val type = resolveType(parentScope, it.type)
        Parameter(
            it.name.located, type,
            buildExpressionGraph(it.defaultValue, parentScope)
        )
    }
    val functionBody = when (val body = functionNode.body) {
        is CodeBlockNode.SingleExpressionCodeBlock -> TODO()//buildExpressionGraph(body.expression, function.bodyScope)
        is CodeBlockNode.StatementsCodeBlock -> body.statements.mapNotNull { buildGraph(innerScope, it) }
        null -> listOf();
    }
    return Function(
        innerScope,
        name?.located,
        resolveType(parentScope, functionNode.returnType),
        parameters,
        functionBody
    );
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

fun resolveType(scope: Scope, type: TypeNode?): Type {
    if (type == null) return Type.Undefined
    return when (type) {
        is TypeNode.ArrayTypeNode -> TODO()
        is TypeNode.FloatTypeNode -> TODO()
        is TypeNode.FunctionTypeSignatureNode -> TODO()
        is TypeNode.IntTypeNode -> Type.Int(type.bitSize)
        is TypeNode.NameTypeNode -> Type.TypeReference(lazy { scope.resolveType(type.name) }, mapOf())
        is TypeNode.StructuralInterfaceTypeNode -> TODO()
        is TypeNode.UnionTypeNode -> TODO()
        is TypeNode.ValueTypeNode -> TODO()
        is TypeNode.VoidTypeNode -> TODO()
    }
}
