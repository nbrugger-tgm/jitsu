package eu.nitok.jitsu.compiler.parser.parsers

import eu.nitok.jitsu.compiler.parser.Tokens

fun parseClass(tokens: Tokens): ClassNode? {
    val classToken = tokens.keyword("class") ?: return null
    tokens.skipWhitespace()
    val name = parseIdentifier(tokens) ?: return null
    tokens.skipWhitespace()
    val typeParameters = parseTypeParameters(tokens)
    tokens.skipWhitespace()
    val extends = parseExtends(tokens)
    tokens.skipWhitespace()
    val implements = parseImplements(tokens)
    tokens.skipWhitespace()
    val body = parseClassBody(tokens)
    return ClassNode(name, typeParameters, extends, implements, body)
}