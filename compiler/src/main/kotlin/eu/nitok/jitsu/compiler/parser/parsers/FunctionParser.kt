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
import java.lang.annotation.Native
import kotlin.jvm.optionals.getOrNull

val fnKeyword = "fn"
val nativeKeyword = "native"
private val fnKeywords = listOf(fnKeyword, "fun", "func", "function")

fun parseFunction(tokens: Tokens): FunctionDeclarationNode? {
    tokens.elevate()
    val native = tokens.keyword(nativeKeyword)
    if(native != null) tokens.skipWhitespace()
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
    val returnType = parseExplicitType(tokens, messages)
    tokens.skipWhitespace()
    val body = parseCodeBlock(tokens)
    if (body == null && native == null) {
        messages.error("Expected function body (starting with '{')", tokens.location.toRange())
    } else if(native != null && body != null) {
        messages.error("Native functions cannot have bodies since they execute C code", body.location,
            Hint("Native declaration", native)
        )
    }
    return FunctionDeclarationNode(
        name = functionName,
        parameters = parameters,
        returnType = returnType,
        body = native.let {
            if(it == null) body
            else FunctionBodyNode.NativeImplementation(it)
        },
        keywordLocation = kw.location,
        attributes = listOf()
    ).withMessages(
        messages
    )
}

private fun parseParameters(
    tokens: Tokens,
    messages: CompilerMessages,
    kw: Located<Tokenizer.AssignedToken<DefaultToken>>
): List<ParameterNode> {
    return tokens.enclosedRepetition(
        BRACKET_OPEN,
        COMMA,
        BRACKET_CLOSED,
        messages,
        "function parameters",
        "parameter"
    ) {
        val argName = parseIdentifier(tokens)
        if (argName == null) {
            val invalid = tokens.range { nextOptional().getOrNull() }
            messages.error("Expected parameter name", invalid.location)
            return@enclosedRepetition null;
        }
        tokens.skipWhitespace()
        val parameterMessages = CompilerMessages()
        val type = parseExplicitType(tokens, parameterMessages)
        ParameterNode(argName, type, null).withMessages(parameterMessages)
    } ?: run {
        messages.error(
            "Expected '(' after function name",
            tokens.location.toRange(),
            Hint("function start", kw.location)
        )
        emptyList()
    }
}