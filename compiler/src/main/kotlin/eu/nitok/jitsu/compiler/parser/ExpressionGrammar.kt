package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar
import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.model.ExpressionType
import eu.nitok.jitsu.compiler.model.ExpressionType.*

const val ANY_EXPRESSION_NAME = "ANY_EXPRESSION";

//every statement can be used as an expression
val statementExpression : Grammar<*> = reference(STATEMENT_NAME).namedCopy(STATEMENT_EXPRESSION)

val enclosedExpression = token(BRACKET_OPEN)
    .then("expression", reference(ANY_EXPRESSION_NAME))
    .then(token(BRACKET_CLOSED)).named(ENCLOSED_EXPRESSION);


val nonRecursiveExpression = arrayOf(statementExpression, literalExpression, enclosedExpression);
val recursiveExpressions = arrayOf(operatorExpression)
val atomicExpressions = arrayOf(literalExpression, enclosedExpression, reference(STATEMENT_EXPRESSION))
val expression = anyOf(
    *recursiveExpressions,
    *nonRecursiveExpression
).named(ANY_EXPRESSION_NAME);
