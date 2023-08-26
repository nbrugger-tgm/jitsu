package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar
import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.model.ExpressionType
import eu.nitok.jitsu.compiler.model.ExpressionType.*


val statementExpression : Grammar<*> = reference(STATEMENT_NAME).namedCopy(STATEMENT_EXPRESSION)

const val ANY_EXPRESSION_NAME = "ANY_EXPRESSION";

val expression = anyOf(
    operatorExpression, statementExpression, literalExpression
).named(ANY_EXPRESSION_NAME);
