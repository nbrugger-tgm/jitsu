package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.grammar.api.GrammarName
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.parser.matchers.ListGrammar

var ANY_TYPE_GRAMMAR = "ANY_TYPE_GRAMMAR";

enum class TypeGrammars :GrammarName {
    ARRAY_TYPE,
    FIXED_VALUE_TYPE,
    NAMED_TYPE,
    UNION_TYPE;
    override fun getName(): String {
        return name
    }
}

val arrayType = reference(ANY_TYPE_GRAMMAR)
    .then(token(SQUARE_BRACKET_OPEN))
    .then(literalExpression)
    .then(token(SQUARE_BRACKET_CLOSED))
    .setLeftRecursive(true)
    .named(TypeGrammars.ARRAY_TYPE);

val genericDeclaration = token(SMALLER).then(ignorables.ignore())
    .then("types",ListGrammar(reference(ANY_TYPE_GRAMMAR), token(COMMA).then(ignorables)))
    .then(ignorables.ignore()).then(token(BIGGER))

val namedType =
    first("mutable", keyword("mut").optional())
        .then(ignorables)
        .then("referenced_type",identifier)
        .then("generic", genericDeclaration.optional())
        .named(TypeGrammars.NAMED_TYPE)

private val nonUnionTypes = arrayOf(
    arrayType,
    namedType,
    anyOf(*valueLiterals).named(TypeGrammars.FIXED_VALUE_TYPE)
);
var unionType = ListGrammar(anyOf(*nonUnionTypes), ignorables.then(keyword("|")).then(ignorables))
    .setMinSize(2)
    .named(TypeGrammars.UNION_TYPE);

var type = anyOf(unionType, *nonUnionTypes).named(ANY_TYPE_GRAMMAR);