package eu.nitok.jitsu.compiler.parser.parsers;

import com.niton.jainparse.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.IdentifierNode
import eu.nitok.jitsu.compiler.parser.Tokens
import eu.nitok.jitsu.compiler.parser.location
import eu.nitok.jitsu.compiler.parser.range

fun parseIdentifier(tokens: Tokens): IdentifierNode {
    val firstToken = tokens.range { tokens.next() }
    val messages = CompilerMessages()
    when (firstToken.value.type) {
        NUMBER, UNDERSCORE, DOLLAR -> messages.error(
            "Identifiers have to start with letters! Numbers, underscores and dollar signs are allowed after the first character",
            firstToken.location
        )

        LETTERS -> {}
        else -> messages.error("Identifiers have to start with letters", firstToken.location)
    }
    var value: String = firstToken.value.value;
    while (true) {
        val token = tokens.peek();
        val type = token.type
        if (type != LETTERS && type != NUMBER && type != UNDERSCORE && type != DOLLAR) {
            break;
        }
        if (type == DOLLAR) {
            messages.warn(
                "Dollar signs are allowed in identifiers, but are discouraged since they are used in auto-generation",
                tokens.location
            )
        }
        value += tokens.next().value;
    }
    return IdentifierNode(firstToken.location.rangeTo(tokens.location), value)
}