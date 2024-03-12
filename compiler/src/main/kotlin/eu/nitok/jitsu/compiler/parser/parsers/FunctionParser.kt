package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import com.niton.jainparse.token.Tokenizer
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.ast.StatementNode.Declaration.*
import eu.nitok.jitsu.compiler.ast.StatementNode.Declaration.FunctionDeclarationNode.*
import eu.nitok.jitsu.compiler.ast.withMessages
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage.Hint
import eu.nitok.jitsu.compiler.parser.*
import kotlin.jvm.optionals.getOrNull

val fnKeyword = "fn"
private val fnKeywords = listOf(fnKeyword, "fun", "func", "function")

fun parseFunction(tokens: Tokens): FunctionDeclarationNode? {
    tokens.elevate()
    val kw = tokens.range { nextOptional().getOrNull() ?: return null; }
    if (kw.value.type != LETTERS || !fnKeywords.contains(kw.value.value)) {
        tokens.rollback()
        return null
    }
    tokens.commit()
    val messages = CompilerMessages()
    if (kw.value.value != fnKeyword) {
        messages.error("Functions are declared with the 'fn' keyword", kw.location)
    }
    tokens.skipWhitespace()
    val functionName = parseIdentifier(tokens)
    if (functionName == null) {
        messages.error(
            CompilerMessage(
                "function requires a name",
                tokens.location.toRange(),
                Hint("Function to name", kw.location)
            )
        )
    }
    tokens.skipWhitespace()
    val parameters = parseParameters(tokens, messages, kw)
    tokens.skipWhitespace()
    val returnType = parseOptionalExplicitType(tokens, messages::error)
    tokens.skipWhitespace()
    val body = parseCodeBlock(tokens)
    if(body == null) {
        messages.error("Expected function body (starting with '{')", tokens.location.toRange())
    }
    return FunctionDeclarationNode(functionName, parameters, returnType, body, kw.location, listOf()).withMessages(
        messages
    )
}

private fun parseParameters(
    tokens: Tokens,
    messages: CompilerMessages,
    kw: Located<Tokenizer.AssignedToken<DefaultToken>>
): MutableList<ParameterNode> {
    val sep = tokens.peekOptional().getOrNull()
    if (sep?.type != BRACKET_OPEN) {
        messages.error(
            "Expected '(' after function name",
            tokens.location.toRange(),
            Hint("function start", kw.location)
        )
    } else tokens.skip()
    val parameters = mutableListOf<ParameterNode>()
    var nonParameterTokens = 0
    paramsLoop@ while (tokens.hasNext()) {
        tokens.skipWhitespace()
        val closedBracket = tokens.peek()
        if (closedBracket.type == BRACKET_CLOSED) {
            tokens.next()
            break
        }
        tokens.skipWhitespace()
        val argName = parseIdentifier(tokens)
        if (argName == null) {
            val invalid = tokens.range { nextOptional().getOrNull() }
            messages.error("Expected parameter name", invalid.location)
            nonParameterTokens++
            if (nonParameterTokens > 2 || invalid.value == null) break
            else continue
        } else nonParameterTokens = 0

        tokens.skipWhitespace()
        val parameterMessages = CompilerMessages()
        val type = parseExplicitType(tokens, parameterMessages)
        tokens.skipWhitespace()
        parameters.add(ParameterNode(argName, type, null).withMessages(parameterMessages))
        tokens.skipWhitespace()
        val commaOrClose = tokens.range { peek() } // Parse either a comma or close parentheses
        if (commaOrClose.value.type == BRACKET_CLOSED) {
            tokens.next()
            break
        } else if (commaOrClose.value.type != COMMA) {
            messages.error("Expected ',' or ')' after argument", commaOrClose.location)
        } else {
            tokens.next()
        }
    }
    return parameters
}