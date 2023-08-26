package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar
import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*

val identifier: Grammar<*> = token(LETTERS).then(
    anyOf(
        token(LETTERS),
        token(NUMBER),
        token(UNDERSCORE)
    ).repeat()
).named("IDENTIFIER")

var ignorables = anyOf(
    token(WHITESPACE),
    token(NEW_LINE)
).repeat();

val TYPE_DEF = token(COLON)
    .then(token(WHITESPACE))
    .then("type", identifier)
    .named("TYPE_DEF")
