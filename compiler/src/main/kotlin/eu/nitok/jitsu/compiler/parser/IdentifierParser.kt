package eu.nitok.jitsu.compiler.parser;
import com.niton.jainparse.token.DefaultToken
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.IdentifierNode
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.Tokens
import eu.nitok.jitsu.compiler.parser.location
import eu.nitok.jitsu.compiler.parser.range

fun parseIdentifier(tokens: Tokens): IdentifierNode {
    val firstToken = tokens.range { tokens.next() }
    val messages = CompilerMessages()
    when (firstToken.value.type) {
        DefaultToken.NUMBER, DefaultToken.UNDERSCORE, DefaultToken.DOLLAR -> messages.error(
            CompilerMessage(
                CompilerMessage.ErrorCode.IDENTIFIER_START_LETTER,
                "Identifiers have to start with letters, numbers, underscores and dollar signs are allowed after the first character",
                firstToken.location
            )
        )

        DefaultToken.LETTERS -> {}
        else -> {
            messages.error(
                CompilerMessage(
                    CompilerMessage.ErrorCode.IDENTIFIER_START_LETTER,
                    "Identifiers have to start with letters",
                    firstToken.location
                )
            )
        }
    }
    var value: String = firstToken.value.value;
    while (true) {
        val token = tokens.peek();
        val type = token.type
        if (type != DefaultToken.LETTERS && type != DefaultToken.NUMBER && type != DefaultToken.UNDERSCORE && type != DefaultToken.DOLLAR) {
            break;
        }
        if (type == DefaultToken.DOLLAR) {
            messages.warn(
                CompilerMessage(
                    CompilerMessage.ErrorCode.IDENTIFIER_CONTAINS_DOLLAR,
                    "Dollar signs are allowed in identifiers, but are discouraged since they are used in auto-generation",
                    tokens.location
                )
            )
        }
        value += tokens.next().value;
    }
    return IdentifierNode(firstToken.location.rangeTo(tokens.location), value)
}
