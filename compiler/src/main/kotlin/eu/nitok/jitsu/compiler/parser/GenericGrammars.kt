package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar
import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*

internal val identifier: Grammar<*> = token(LETTERS).then(
    anyOf(
        token(LETTERS),
        token(NUMBER),
        token(UNDERSCORE)
    ).repeat()
).merged().named("IDENTIFIER").display("identifier")

internal var ignorables = anyOf(
    token(WHITESPACE),
    token(NEW_LINE)
).repeat();
