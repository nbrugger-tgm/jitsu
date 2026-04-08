package eu.nitok.jitsu.parser.parsers

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.parser.ast.ExpressionNode
import eu.nitok.jitsu.parser.ast.ExpressionNode.StringLiteralNode
import eu.nitok.jitsu.common.Located
import eu.nitok.jitsu.parser.ast.withMessages
import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessage.Hint
import eu.nitok.jitsu.common.Range
import eu.nitok.jitsu.compiler.model.BiOperator
import eu.nitok.jitsu.parser.*
import kotlin.jvm.optionals.getOrNull

/**
 * Parses any expression from the token stream. Handles simple expressions (literals, variables)
 * and composite expressions (binary operations) with left-associative chaining.
 *
 * Examples: `42`, `"hello"`, `myVar`, `1 + 2`, `a + b * c` (parsed as `(a + b) * c`)
 *
 * @return The parsed expression node, or null if no valid expression starts at the current position.
 */
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
        val terminationChar = stream.attempt(DOUBLEQUOTE, EOF, NEW_LINE);
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
    return StringLiteralNode(string, openingQuote.location.rangeTo(termination!!)).withMessages(messages)
}

/**
 * Parses a single component of a string literal (escape sequence, interpolation, or character sequence).
 */
fun parseStringPart(stream: Tokens): StringLiteralNode.StringPart? {
    if (!stream.hasNext()) return null
    return parseEscapeCharacterStringPart(stream) ?: parseExpressionStringPart(stream) ?: parseLiteralStringPart(stream)
    ?: parseCharSequenceStringPart(stream);
}


/**
 * Parses an escape sequence in a string literal (e.g., `\n`, `\\`, `\t`).
 */
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

/**
 * Parses a plain character sequence within a string literal (text between special characters).
 */
fun parseCharSequenceStringPart(stream: Tokens): StringLiteralNode.StringPart? {
    stream.elevate()
    val chars = stream.captureUntil(EOF, NEW_LINE, DOLLAR, BACK_SLASH, DOUBLEQUOTE)
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

/**
 * Parses simple string interpolation (`$name`) within a string literal.
 */
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

/**
 * Parses expression interpolation (`${expr}`) within a string literal.
 */
fun parseExpressionStringPart(stream: Tokens): StringLiteralNode.StringPart? {
    val kw = stream.attempt {
        nullableRange { if(next().type == DOLLAR && next().type == ROUND_BRACKET_OPEN) "\${" else null}
    } ?: return null;
    val exp = parseExpression(stream);
    val closingKw = stream.attempt(ROUND_BRACKET_CLOSED);
    return StringLiteralNode.StringPart.Expression(exp, kw.location, closingKw?.location)
        .run {
            if (closingKw == null) this.error(
                CompilerMessage(
                    "String template expression wasn't closed",
                    stream.location.toRange(),
                    listOf(Hint("expression opened", kw.location))
                )
            )
            this
        }
}

/**
 * Parses a binary operation following a left operand (e.g., `+ 2` after `1`).
 * Supports `+`, `-`, `*`, `/` operators.
 *
 * @param left The already-parsed left operand of the operation.
 * @return An OperationNode combining left with the parsed operator and right operand,
 *         or null if no operator is present.
 */
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

/**
 * Parses an integer literal with optional sign.
 *
 * Examples: `123`, `-42`, `+100`, `0`
 *
 * @return An IntegerLiteralNode, or null if no integer literal is present.
 */
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
