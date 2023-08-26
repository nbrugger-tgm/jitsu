package eu.nitok.jitsu.compiler.parser

import com.niton.parser.ast.SequenceNode
import com.niton.parser.grammar.api.Grammar
import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken
import com.niton.parser.token.Tokenable
import eu.nitok.jitsu.compiler.model.ExpressionType
import eu.nitok.jitsu.compiler.model.OperationExpressionType

private val nonRecursive = anyOf(statementExpression,literalExpression);

private val addExpression = mathExtensionExpression(OperationExpressionType.ADD_EXPRESSION, DefaultToken.PLUS);
private val multiplyExpression = mathExtensionExpression(OperationExpressionType.MULTIPLY_EXPRESSION, DefaultToken.STAR);
private val subtractExpression = mathExtensionExpression(OperationExpressionType.SUBTRACT_EXPRESSION, DefaultToken.MINUS);
private val divideExpression = mathExtensionExpression(OperationExpressionType.DIVIDE_EXPRESSION, DefaultToken.SLASH);
private fun mathExtensionExpression(name: OperationExpressionType, token: Tokenable): Grammar<SequenceNode>? {
    return build(name)
        .grammar(nonRecursive).add("left")
        .token(DefaultToken.WHITESPACE).ignore().add()
        .token(token).add()
        .token(DefaultToken.WHITESPACE).ignore().add()
        .grammar(ANY_EXPRESSION_NAME).add("right")
        .get()
}

var operatorExpression = anyOf(
    addExpression,
    multiplyExpression,
    subtractExpression,
    divideExpression
).named(ExpressionType.OPERATION_EXPRESSION);