package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar
import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.ExpressionType
import eu.nitok.jitsu.compiler.ast.ExpressionType.*

const val ANY_EXPRESSION_NAME = "ANY_EXPRESSION";

//every statement can be used as an expression
internal val statementExpression : Grammar<*> = reference(STATEMENT_NAME).namedCopy(STATEMENT_EXPRESSION)

internal val enclosedExpression = token(BRACKET_OPEN)
    .then("expression", reference(ANY_EXPRESSION_NAME))
    .then(token(BRACKET_CLOSED))
    .named(ENCLOSED_EXPRESSION).display("expression in brackets");

internal val fieldAccess = first("target", reference(ANY_EXPRESSION_NAME))
    .then(ignorables.ignore())
    .then(token(POINT))
    .then("field", identifier)
    .setLeftRecursive(true)
    .named(FIELD_ACCESS_EXPRESSION)
    .display("field access");


internal val nonRecursiveExpression = arrayOf(statementExpression, fieldAccess, literalExpression, enclosedExpression);
internal val recursiveExpressions = arrayOf(operatorExpression)
internal val atomicExpressions = arrayOf(literalExpression, enclosedExpression, reference(STATEMENT_EXPRESSION))
internal val expression = anyOf(
    *recursiveExpressions,
    *nonRecursiveExpression
).named(ANY_EXPRESSION_NAME);