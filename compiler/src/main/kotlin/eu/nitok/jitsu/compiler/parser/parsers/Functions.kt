package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.StatementNode.FunctionDeclarationNode
import eu.nitok.jitsu.compiler.ast.StatementNode.FunctionDeclarationNode.ParameterNode
import eu.nitok.jitsu.compiler.ast.withMessages
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.*

val fnKeyword = "fn"
private val fnKeywords = listOf(fnKeyword,"fun", "func", "function")

fun parseFunction(tokens: Tokens): FunctionDeclarationNode? {
    tokens.elevate()
    if(!tokens.hasNext()) return null;
    val kw = tokens.range { next() };
    if (kw.value.type != LETTERS || !fnKeywords.contains(kw.value.value)) {
        tokens.rollback()
        return null
    }
    tokens.commit()
    val messages = CompilerMessages()
    if (kw.value.value != fnKeyword) {
        messages.error("Functions are declared with the 'fn' keyword", kw.location);
    }
    tokens.skip(WHITESPACE)
    val functionName = parseIdentifier(tokens) // Parse function name
    tokens.skip(WHITESPACE)
    val returnType = parseExplicitType(tokens);
    val sep = tokens.peek()
    if (sep.type != BRACKET_OPEN) {
        messages.error(
            "Expected '(' after function name",
            tokens.location,
            CompilerMessage.Hint("function start", kw.location)
        )
    } else {
        tokens.next()
    }
    tokens.skip(WHITESPACE)
    val parameters = mutableListOf<ParameterNode>()
    while (true) {
        val next = tokens.peek()
        if (next.type == BRACKET_CLOSED) {
            tokens.next()
            break;
        }
        tokens.skip(WHITESPACE)
        val argName = parseIdentifier(tokens)
        tokens.skip(WHITESPACE)
        val type = parseExplicitType(tokens)
        tokens.skip(WHITESPACE)
        parameters.add(ParameterNode(argName, type, null))
        tokens.skip(WHITESPACE)
        val commaOrClose = tokens.range { peek() } // Parse either a comma or close parentheses
        if (commaOrClose.value.type == BRACKET_CLOSED) {
            tokens.next()
            break
        }
        else if (commaOrClose.value.type != COMMA) {
            messages.error("Expected ',' or ')' after argument", commaOrClose.location)
        } else {
            tokens.next()
        }
    }
    return FunctionDeclarationNode(functionName, parameters, returnType, null, kw.location, listOf()).withMessages(messages)
}