package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.model.ExpressionType
import eu.nitok.jitsu.compiler.model.LiteralType

val numberLiteral = token(NUMBER).named(LiteralType.NUMBER_LITERAL);

private val escapedBackslash = token(BACK_SLASH).then(token(BACK_SLASH));
private val escapeSequence = anyOf(
    keyword("\\n"),
    keyword("\\t"),
    keyword("\\r"),
    keyword("\\\\"),
    keyword("\\\""),
    keyword("\\\'"),
);
private val stringTerminatingTokens = token(BACK_SLASH).or(token(DOUBLEQUOTE)).or(token(NEW_LINE)).or(token(EOF))
private val stringContent = anyExcept(stringTerminatingTokens).then(escapeSequence.repeat()).repeat()
val stringLiteral = token(DOUBLEQUOTE).then("content", stringContent).then(token(DOUBLEQUOTE)).named(LiteralType.STRING_LITERAL);

var valueLiterals = arrayOf(
    numberLiteral,
    stringLiteral
);

val variableLiteral = identifier.namedCopy(LiteralType.VARIABLE_LITERAL)
val literalExpression = anyOf(
    *valueLiterals,
    variableLiteral
).named(ExpressionType.LITERAL_EXPRESSION);