package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.IdentifierNode
import eu.nitok.jitsu.compiler.ast.withMessages
import eu.nitok.jitsu.compiler.parser.*
import kotlin.jvm.optionals.getOrElse

fun parseIdentifier(tokens: Tokens): IdentifierNode? {
    val firstToken = tokens.range {
        tokens.peekOptional().getOrElse { return null }
    }
    val messages = CompilerMessages()
    when (firstToken.value.type) {
        NUMBER, UNDERSCORE, DOLLAR -> messages.error(
            "Identifiers have to start with letters! Numbers, underscores and dollar signs are allowed after the first character",
            firstToken.location
        )

        LETTERS -> {}
        else -> return null
    }
    tokens.skip()
    var value: String = firstToken.value.value
    while (tokens.hasNext()) {
        val token = tokens.peek()
        val type = token.type
        if (type != LETTERS && type != NUMBER && type != UNDERSCORE && type != DOLLAR) {
            break
        }
        if (type == DOLLAR) {
            messages.warn(
                "Dollar signs are allowed in identifiers, but are discouraged since they are used in auto-generation",
                tokens.location.toRange()
            )
        }
        value += tokens.next().value
    }
    return IdentifierNode(firstToken.location.rangeTo(tokens.lastConsumedLocation), value).withMessages(messages)
}
