package eu.nitok.jitsu.parser.parsers

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.DefaultToken.*
import com.niton.jainparse.token.Tokenizer
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.parser.ast.StatementNode.Declaration.*
import eu.nitok.jitsu.parser.ast.StatementNode.Declaration.FunctionDeclarationNode.*
import eu.nitok.jitsu.parser.ast.withMessages
import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessage.Hint
import eu.nitok.jitsu.parser.*
import eu.nitok.jitsu.parser.ast.AttributeNode
import kotlin.jvm.optionals.getOrNull

/** The canonical keyword for function declarations. */
val fnKeyword = "fn"
/** The keyword for native (external) functions. */
val nativeKeyword = "native"
private val fnKeywords = listOf(fnKeyword, "fun", "func", "function")

/**
 * Parses a function declaration starting with `fn` (or similar keywords).
 *
 * Syntax: `[native] fn <name>([params])[: returnType] [{ body }]`
 *
 * Examples:
 * - `fn foo() {}`
 * - `fn bar(x: i32): i64 {}`
 * - `native fn external()`
 *
 * Note: Alternative keywords (`fun`, `func`, `function`) are accepted but produce an error
 * recommending the use of `fn`.
 *
 * @return A FunctionDeclarationNode, or null if no function keyword is present.
 */
internal fun parseFunction(tokens: Tokens, attributes: List<AttributeNode>): FunctionDeclarationNode? {
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
                tokens.position.toLocation(),
                Hint("Function to name", kw.location)
            )
        )
    }
    tokens.skipWhitespace()
    val parameters = parseParameters(tokens, messages, kw)
    tokens.skipWhitespace()
    val returnType = parseExplicitType(tokens, messages, lenient = false)
    tokens.skipWhitespace()
    val body = parseCodeBlock(tokens)
    if (body == null && native == null) {
        messages.error("Expected function body (starting with '{')", tokens.position.toLocation())
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
        attributes = attributes
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
        val type = parseExplicitType(tokens, parameterMessages, explicitTypeRequiredMessage = "Parameter types need to be explicitly defined ': paramtype'")
        ParameterNode(argName, type, null).withMessages(parameterMessages)
    }?.elements ?: run {
        messages.error(
            "Expected '(' after function name",
            tokens.position.toLocation(),
            Hint("function start", kw.location)
        )
        emptyList()
    }
}
