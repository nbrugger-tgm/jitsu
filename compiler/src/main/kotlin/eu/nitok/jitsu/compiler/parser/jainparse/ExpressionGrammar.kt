package eu.nitok.jitsu.compiler.parser.jainparse

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.ExpressionType.*

const val ANY_EXPRESSION_NAME = "ANY_EXPRESSION";

internal val enclosedExpression = token(BRACKET_OPEN)
    .then("expression", reference(ANY_EXPRESSION_NAME))
    .then(token(BRACKET_CLOSED))
    .named(ENCLOSED_EXPRESSION).display("expression in brackets");

internal val fieldAccess = first("target", reference(ANY_EXPRESSION_NAME))
    .then(ignorables.ignore())
    .then(token(DOT))
    .then("field", identifier)
    .setLeftRecursive(true)
    .named(FIELD_ACCESS_EXPRESSION)
    .display("field access");

internal val indexAccess = first("array", reference(ANY_EXPRESSION_NAME))
    .then(ignorables.ignore())
    .then(token(SQUARE_BRACKET_OPEN))
    .then("index", reference(ANY_EXPRESSION_NAME))
    .then(token(SQUARE_BRACKET_CLOSED))
    .setLeftRecursive(true)
    .named(INDEXED_ACCESS_EXPRESSION)
    .display("array access");


internal val nonRecursiveExpression = arrayOf(
    reference(METHOD_INVOCATION),
    fieldAccess,
    indexAccess,
    reference(STATEMENT_EXPRESSION),
    literalExpression,
    enclosedExpression
);
internal val recursiveExpressions = arrayOf(operatorExpression)
internal val expression = anyOf(
    *recursiveExpressions,
    *nonRecursiveExpression
).named(ANY_EXPRESSION_NAME);
