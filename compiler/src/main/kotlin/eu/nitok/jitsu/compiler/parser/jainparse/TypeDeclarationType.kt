package eu.nitok.jitsu.compiler.parser.jainparse

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.TypeDeclarationType
import eu.nitok.jitsu.compiler.parser.jainparse.matchers.ListGrammar

var ANY_TYPE_GRAMMAR = "ANY_TYPE_GRAMMAR";

internal val arrayType = first("type", reference(ANY_TYPE_GRAMMAR))
    .then(token(SQUARE_BRACKET_OPEN))
    .then("fixed_size", numberLiteral.optional())
    .then(token(SQUARE_BRACKET_CLOSED))
    .setLeftRecursive(true)
    .named(TypeDeclarationType.ARRAY_TYPE)
    .display("array type")

internal val genericDeclaration = token(SMALLER).then(ignorables.ignore())
    .then("types", ListGrammar(reference(ANY_TYPE_GRAMMAR), token(COMMA).then(ignorables)))
    .then(ignorables.ignore()).then(token(BIGGER))
    .display("generics")

internal val namedType =
    first("mutable", keyword("mut").optional())
        .then(ignorables)
        .then("referenced_type", identifier)
        .then("generic", genericDeclaration.optional())
        .named(TypeDeclarationType.NAMED_TYPE)
        .display("type")

internal val enumType =
    first("keyword", keyword("enum"))
        .then(token(BRACKET_OPEN))
        .then(ignorables.ignore())
        .then("constants", ListGrammar(identifier, token(COMMA).then(ignorables)))
        .then(token(BRACKET_CLOSED))
        .named(TypeDeclarationType.ENUM_TYPE)
        .display("enum")

private val nonUnionTypes = arrayOf(
    arrayType,
    enumType,
    namedType,
    anyOf(*valueLiterals).named(TypeDeclarationType.FIXED_VALUE_TYPE)
);
internal var unionType = ListGrammar(anyOf(*nonUnionTypes), ignorables.then(keyword("|")).then(ignorables))
    .setMinSize(2)
    .named(TypeDeclarationType.UNION_TYPE)
    .display("union");

internal var type = anyOf(unionType, *nonUnionTypes).named(ANY_TYPE_GRAMMAR);