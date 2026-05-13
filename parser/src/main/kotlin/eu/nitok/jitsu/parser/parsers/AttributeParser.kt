package eu.nitok.jitsu.parser.parsers

import com.niton.jainparse.token.DefaultToken
import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.parser.*
import eu.nitok.jitsu.parser.ast.AttributeDeclarationNode
import eu.nitok.jitsu.parser.ast.AttributeNode
import eu.nitok.jitsu.parser.ast.withMessages


internal fun parseAttributes(tokens: Tokens): MutableList<AttributeNode> {
    val attributes = mutableListOf<AttributeNode>()
    while (true) {
        val attr = parseAttributeUse(tokens) ?: break
        attributes.add(attr)
        tokens.skipWhitespace()
    }
    return attributes
}

internal fun parseAttributeUse(tokens: Tokens): AttributeNode? {
    val openKw = tokens.attempt(DefaultToken.SQUARE_BRACKET_OPEN) ?: return null
    tokens.skipWhitespace()
    val messages = CompilerMessages()
    val attributeName = parseIdentifier(tokens)
    if (attributeName == null) {
        messages.error("Expected attribute name", tokens.position)
    }
    tokens.skipWhitespace()
    val props = tokens.enclosedRepetition(
        start = DefaultToken.BRACKET_OPEN,
        delimitter = DefaultToken.COMMA,
        end = DefaultToken.BRACKET_CLOSED,
        messages = messages,
        subject = "attribute properties",
        elementName = "attribute property",
        doNotSkip = setOf(DefaultToken.SQUARE_BRACKET_CLOSED)
    ) { parseAttributeUseParameter(it) }
    val expectedCloseKwPos = tokens.position
    tokens.skipWhitespace()
    val closwKw = tokens.attempt(DefaultToken.SQUARE_BRACKET_CLOSED)
    if (closwKw == null) messages.error(
        "Expected closing ']' of attribute", expectedCloseKwPos,
        CompilerMessage.Hint("Attribute started here", openKw)
    )
    return AttributeNode(attributeName, props?.elements ?: listOf(), openKw.location, closwKw?.location).withMessages(
        messages
    )
}

private fun parseAttributeUseParameter(tokens: Tokens): AttributeNode.AttributeValueNode? {
    val property = parseIdentifier(tokens) ?: return null
    val messages = CompilerMessages()
    tokens.skipWhitespace()
    tokens.attempt(DefaultToken.EQUAL) ?: run {
        messages.error("Expected '='", tokens.position)
    }
    tokens.skipWhitespace()
    val value = parseExpression(tokens)
    if (value == null) {
        messages.error("Expected literal after '='", tokens.position)
    }
    return AttributeNode.AttributeValueNode(property, value).withMessages(messages)
}

internal fun parseAttributeDeclaration(tokens: Tokens): AttributeDeclarationNode? {
    val kw = tokens.keyword("attribute") ?: return null
    val messages = CompilerMessages()
    tokens.skipWhitespace()
    val name = parseIdentifier(tokens)
    if (name == null) messages.error("Expected attribute name", tokens.position)
    else tokens.skipWhitespace()
    val bodyStart = tokens.attempt(DefaultToken.ROUND_BRACKET_OPEN) ?: return AttributeDeclarationNode(
        name,
        emptyList(),
        kw,
        null,
        null
    ).withMessages(messages)
    tokens.skipWhitespace()
    val properties = mutableListOf<AttributeDeclarationNode.AttributeProperty>()
    do {
        tokens.skipWhitespace()
        val prop = parseAttributeProperty(tokens)
        prop?.let { properties.add(it) }
    } while (prop != null)
    tokens.skipWhitespace()
    val bodyEnd = tokens.attempt(DefaultToken.ROUND_BRACKET_CLOSED)
    if (bodyEnd == null) messages.error(
        "Expected closing '}'",
        tokens.position,
        CompilerMessage.Hint("Attribute body started here", bodyStart)
    )
    return AttributeDeclarationNode(name, properties, kw, bodyStart.location, bodyEnd?.location).withMessages(messages)
}

private fun parseAttributeProperty(tokens: Tokens): AttributeDeclarationNode.AttributeProperty? {
    val messages = CompilerMessages()
    val name = parseIdentifier(tokens)
    val expectedNameLocation = name?.location?:tokens.position
    if (name != null) tokens.skipWhitespace()
    val typeDefiniton = parseExplicitType(tokens, messages, lenient = name != null,
        explicitTypeRequiredMessage = "Attribute properties require explicit types ': type'"
    )
    if (name == null && typeDefiniton == null) {
        messages.error("Expected attribute property", expectedNameLocation)
        return null
    }
    if(name == null) {
        messages.error("Expected attribute property name", expectedNameLocation)
    }
    tokens.skipWhitespace()
    val semicolon = tokens.attempt(DefaultToken.SEMICOLON)
    if (semicolon == null) messages.error("Expected semicolon at end of attribute property", tokens.position)
    return AttributeDeclarationNode.AttributeProperty(name, typeDefiniton).withMessages(messages)
}
