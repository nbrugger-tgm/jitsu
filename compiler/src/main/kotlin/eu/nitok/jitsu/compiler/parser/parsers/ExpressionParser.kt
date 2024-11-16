package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.BiOperator
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.ExpressionNode
import eu.nitok.jitsu.compiler.ast.ExpressionNode.StringLiteralNode
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.ast.withMessages
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage.Hint
import eu.nitok.jitsu.compiler.parser.*
import kotlin.jvm.optionals.getOrNull

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
    parseIntLiteral(tokens) ?: parseStringLiteral(tokens) ?: parseIdentifierBased(tokens) { tokens, identifier ->
        parseFunctionCall(tokens, identifier) ?: ExpressionNode.VariableReferenceNode(identifier)
    }

private fun parseStringLiteral(stream: Tokens): ExpressionNode? {
    val openingQuote = stream.attempt(DOUBLEQUOTE) ?: return null;
    var termination: Range? = null;
    var string = mutableListOf<StringLiteralNode.StringPart>()
    var messages = CompilerMessages()
    while (true) {
        val terminationChar = stream.nullableRange {  peekOptional().getOrNull() };
        if (terminationChar != null) {
            termination = terminationChar.location
            if (terminationChar.value.type != DOUBLEQUOTE) {
                messages.error(
                    "String interrupted by ${terminationChar.value.type}",
                    stream.location.toRange(),
                    Hint("String opened", openingQuote.location)
                )
            } else {
                stream.skip()
            }
            break;
        }
        val part = parseStringPart(stream) ?: break;
        string.add(part)
    }
    if (termination == null) {
        messages.error(
            "String interrupted by EOF",
            stream.location.toRange(),
            Hint("String opened", openingQuote.location)
        )
        termination = stream.location.toRange()
    }
    return StringLiteralNode(string, openingQuote.location.rangeTo(termination)).withMessages(messages)
}

fun parseStringPart(stream: Tokens): StringLiteralNode.StringPart? {
    if (!stream.hasNext()) return null
    return parseEscapeCharacterStringPart(stream) ?: parseExpressionStringPart(stream) ?: parseLiteralStringPart(stream)
    ?: parseCharSequenceStringPart(stream);
}


fun parseEscapeCharacterStringPart(stream: Tokens): StringLiteralNode.StringPart? {
    var kw = stream.attempt(BACK_SLASH) ?: return null;
    var escaped = stream.nullableRange { splice(1) }

    var escape = StringLiteralNode.StringPart.EscapeSequence(
        escaped?.value?.value ?: "",
        kw.location.rangeTo(escaped?.location ?: kw.location)
    )
    if (escaped == null) {
        escape.error(
            CompilerMessage(
                "Missing escape sequence: \\ is a special character. Its used for escaping special chars (like \\n for newline). If you want to have a \\ in your string escape the backslash (\\\\)",
                kw.location
            )
        )
    }
    return escape;
}

fun parseCharSequenceStringPart(stream: Tokens): StringLiteralNode.StringPart? {
    stream.elevate()
    var chars = stream.captureUntil(EOF, NEW_LINE, DOLLAR, BACK_SLASH)
    if (chars.value.isEmpty()) {
        stream.rollback()
        return null
    } else stream.commit()
    return StringLiteralNode.StringPart.CharSequence(chars.value, chars.location)
}

private fun Tokens.captureUntil(vararg token: DefaultToken): Located<String> {
    return range {
        var builder = StringBuilder()
        while (peekOptional().getOrNull()?.type?.let { token.contains(it) } == false) {
            builder.append(next().value)
        }
        builder.toString()
    }
}

fun parseLiteralStringPart(stream: Tokens): StringLiteralNode.StringPart? {
    var kw = stream.attempt(DefaultToken.DOLLAR) ?: return null;
    var literal =
        parseIdentifierBased(stream) { tokens, identifier -> ExpressionNode.VariableReferenceNode(identifier) }
    return StringLiteralNode.StringPart.VarReference(literal, kw.location).run {
        if (literal == null) {
            this.error(
                CompilerMessage(
                    "\$ is used for string interpolation (\"My name is \$name\"). the name of the value is missing, either escape the \$ with a \\ or add a variable name to substitute",
                    kw.location
                )
            )
        }
        this
    };
}

fun parseExpressionStringPart(stream: Tokens): StringLiteralNode.StringPart? {
    var kw = stream.keyword("\${") ?: return null;
    var exp = parseExpression(stream);
    var closingKw = stream.attempt(DefaultToken.ROUND_BRACKET_CLOSED);
    return StringLiteralNode.StringPart.Expression(exp, kw, closingKw?.location ?: exp?.location ?: kw)
        .run {
            if (closingKw == null) this.error(
                CompilerMessage(
                    "String template expression wasn't closed",
                    stream.location.toRange(),
                    listOf(Hint("expression opened", kw))
                )
            )
            this
        }
}

fun parseOperation(tokens: Tokens, left: ExpressionNode): ExpressionNode? {
    tokens.elevate()
    tokens.skipWhitespace()
    val op = tokens.attempt(PLUS, DefaultToken.MINUS, DefaultToken.STAR, DefaultToken.SLASH)
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
    var literal = tokens.nullableRange {
        attempt {
            val sign = tokens.attempt(PLUS, MINUS)
            skip(WHITESPACE)
            val number = tokens.attempt(NUMBER) ?: return@attempt null
            if (sign != null) sign.value.value + number.value.value
            else number.value.value
        }
    } ?: return null;
    return ExpressionNode.NumberLiteralNode.IntegerLiteralNode(literal.value, literal.location)
}
