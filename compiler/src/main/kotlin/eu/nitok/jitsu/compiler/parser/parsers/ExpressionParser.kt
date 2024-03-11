package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken
import eu.nitok.jitsu.compiler.ast.BiOperator
import eu.nitok.jitsu.compiler.ast.ExpressionNode
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.parser.*

fun parseExpression(tokens: Tokens): ExpressionNode? {
    var expressionNode = parseSingleExpression(tokens)
    while (expressionNode != null) {
        val composite = parseCompositExpression(tokens, expressionNode) ?: return expressionNode
        expressionNode = composite
    }
    return expressionNode
}
private fun parseCompositExpression(
    tokens: Tokens,
    expressionNode: ExpressionNode
) = parseOperation(tokens, expressionNode)

private fun parseSingleExpression(tokens: Tokens) =
    parseIntLiteral(tokens) ?: parseIdentifierBased(tokens) { tokens, identifier ->
        parseFunctionCall(tokens, identifier) ?: ExpressionNode.VariableReferenceNode(identifier)
    }

fun parseOperation(tokens: Tokens, left: ExpressionNode): ExpressionNode? {
    tokens.elevate()
    tokens.skipWhitespace()
    val op = tokens.expect(DefaultToken.PLUS, DefaultToken.MINUS, DefaultToken.STAR, DefaultToken.SLASH)
    if (op == null) {
        tokens.rollback()
        return null
    }
    tokens.commit()
    tokens.skipWhitespace()
    val right = parseSingleExpression(tokens)
    return ExpressionNode.OperationNode(
        left,
        Located(
            BiOperator.byRune(op.value.value)
                ?: throw IllegalStateException("No operator found for rune ${op.value.value}!"), op.location
        ),
        right
    )
}

fun parseIntLiteral(tokens: Tokens): ExpressionNode? {
    val next = tokens.expect(DefaultToken.NUMBER) ?: return null
    return ExpressionNode.NumberLiteralNode.IntegerLiteralNode(next.value.value, next.location)
}
