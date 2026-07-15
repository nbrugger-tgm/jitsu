package eu.nitok.jitsu.parser.parsers

import com.niton.jainparse.token.DefaultToken.*
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.parser.Tokens
import eu.nitok.jitsu.parser.ast.IdentifierNode
import eu.nitok.jitsu.parser.ast.withMessages
import eu.nitok.jitsu.parser.attempt
import eu.nitok.jitsu.parser.lastConsumedLocation
import eu.nitok.jitsu.parser.position

internal fun parseIdentifier(tokens: Tokens): IdentifierNode? {
    val firstToken = tokens.attempt(NUMBER, UNDERSCORE, DOLLAR, LETTERS)?: return null
    val messages = CompilerMessages()
    when (firstToken.value.type) {
        NUMBER, UNDERSCORE, DOLLAR -> messages.error(
            "Identifiers have to start with letters! Numbers, underscores and dollar signs are allowed after the first character",
            firstToken.location
        )

        LETTERS -> {}
        else -> error("This should never happen")
    }
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
                tokens.position.toLocation()
            )
        }
        value += tokens.next().value
    }
    return IdentifierNode(firstToken.location.rangeTo(tokens.lastConsumedLocation), value).withMessages(messages)
}
