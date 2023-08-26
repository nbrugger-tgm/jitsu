package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar
import com.niton.parser.grammar.api.Grammar.anyOf
import com.niton.parser.token.DefaultToken
import eu.nitok.jitsu.compiler.model.ExpressionType
import eu.nitok.jitsu.compiler.model.LiteralType

private val numberLiteral = Grammar.token(DefaultToken.NUMBER).named(LiteralType.NUMBER_LITERAL);

val variableLiteral = identifier.namedCopy(LiteralType.VARIABLE_LITERAL)
val literalExpression = anyOf(
    numberLiteral,
    variableLiteral
).named(ExpressionType.LITERAL_EXPRESSION);