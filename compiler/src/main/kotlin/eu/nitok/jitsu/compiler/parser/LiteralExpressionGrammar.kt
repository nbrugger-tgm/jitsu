package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.ExpressionType
import eu.nitok.jitsu.compiler.ast.LiteralType

internal val numberLiteral = token(NUMBER).named(LiteralType.NUMBER_LITERAL);

internal val booleanLiteral = anyOf(
    keyword("true"),
    keyword("false")
).named(LiteralType.BOOLEAN_LITERAL).display("boolean");

private val escapedBackslash = token(BACK_SLASH).then(token(BACK_SLASH));
private val escapeSequence = anyOf(
    keyword("\\n"),
    keyword("\\t"),
    keyword("\\r"),
    keyword("\\\\"),
    keyword("\\\""),
    keyword("\\\'"),
).display("escape sequence");
private val stringTerminatingTokens = token(BACK_SLASH).or(token(DOUBLEQUOTE)).or(token(NEW_LINE)).or(token(EOF))
private val stringContent = anyExcept(stringTerminatingTokens).then(escapeSequence.repeat()).repeat()
internal val stringLiteral = token(DOUBLEQUOTE).then("content", stringContent).then(token(DOUBLEQUOTE))
    .named(LiteralType.STRING_LITERAL)
    .display("string");

internal var valueLiterals = arrayOf(
    numberLiteral,
    stringLiteral,
    booleanLiteral
);

internal val variableLiteral = identifier.namedCopy(LiteralType.VARIABLE_LITERAL).display("variable");
internal val literalExpression = anyOf(
    *valueLiterals,
    variableLiteral
).named(ExpressionType.LITERAL_EXPRESSION).display("literal");