package eu.nitok.jitsu.compiler.parser

import com.niton.parser.DefaultParser
import com.niton.parser.ast.LocatableReducedNode
import com.niton.parser.grammar.api.Grammar.token
import com.niton.parser.grammar.api.GrammarReferenceMap
import com.niton.parser.token.DefaultToken.*

private val fileGrammar =
    codeLines
        .then(token(EOF).optional())
        .named("JITSU_FILE")

private val parser = DefaultParser(
    GrammarReferenceMap()
        .deepMap(fileGrammar)
        .deepMap(expression)
        .deepMap(statement),
    fileGrammar
);

fun parse(file: String): LocatableReducedNode {
    return parser.parse(file).reduce("FILE").orElseThrow();
}