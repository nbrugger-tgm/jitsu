package eu.nitok.jitsu.compiler.parser.jainparse

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.ExpressionType
import eu.nitok.jitsu.compiler.ast.LiteralType
import eu.nitok.jitsu.compiler.ast.StringTemplateType.TEMPLATE_EXPRESSION
import eu.nitok.jitsu.compiler.ast.StringTemplateType.TEMPLATE_LITERAL

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
    keyword("\\$"),
    keyword("\\\\"),
    keyword("\\\""),
    keyword("\\\'")
).display("escape sequence");

private val stringTemplate = anyOf(
    first("symbol", token(DOLLAR)).then("variable", identifier).named(TEMPLATE_LITERAL),
    first("symbol", keyword("\${")).then("expression", reference(ANY_EXPRESSION_NAME))
        .then("close_symbol", token(ROUND_BRACKET_CLOSED)).named(TEMPLATE_EXPRESSION)
).named(TEMPLATE_EXPRESSION).display("template expression");


private val stringTerminatingTokens = token(BACK_SLASH).or(token(DOUBLEQUOTE)).or(token(NEW_LINE)).or(token(DOLLAR)).or(token(EOF))
private val stringContent =
    first("chars", anyExcept(stringTerminatingTokens))
    .then("template_expression", stringTemplate.optional())
    .then("escape_sequence", escapeSequence.optional())
    .repeat()
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