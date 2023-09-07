package eu.nitok.jitsu.compiler.parser.jainparse

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken
import com.niton.parser.token.DefaultToken.COLON
import com.niton.parser.token.DefaultToken.SEMICOLON
import eu.nitok.jitsu.compiler.ast.CaseBodyType
import eu.nitok.jitsu.compiler.ast.CaseMatchType
import eu.nitok.jitsu.compiler.ast.CaseMatchingType
import eu.nitok.jitsu.compiler.ast.StatementType
import eu.nitok.jitsu.compiler.parser.jainparse.matchers.ListGrammar

private val constantMatch = literalExpression.namedCopy(CaseMatchType.CONSTANT_CASE);

internal val DECONSTRUCT_MATCH = token(DefaultToken.BRACKET_OPEN)
    .then(ignorables.ignore())
    .then("variables", ListGrammar(identifier, token(DefaultToken.COMMA).then(ignorables)))
    .then(ignorables.ignore())
    .then(token(DefaultToken.BRACKET_CLOSED))
    .named(CaseMatchingType.DECONSTRUCT_PATTERN_MATCH)
internal val TYPECAST_MATCH = identifier.namedCopy(CaseMatchingType.CASTING_PATTERN_MATCH)
private val typematch =
    first("type", type)
        .then(ignorables.ignore())
        .then("matching", DECONSTRUCT_MATCH.or(TYPECAST_MATCH))

private val conditionMatch = typematch.then(optional(expression)).named(CaseMatchType.CONDITION_CASE)
private val defaultMatch = first("keyword", keyword("default")).named(CaseMatchType.DEFAULT_CASE);

private val caseCodeBlockBody = token(COLON).then(ignorables).then("body", codeBlock)
    .named(CaseBodyType.CODE_BLOCK_CASE_BODY)
private val expressionBlockBody = keyword("->").then(ignorables).then("body", expression).then(token(SEMICOLON))
    .named(CaseBodyType.EXPRESSION_CASE_BODY)

private val case = first("keyword", keyword("case"))
    .then(ignorables)
    .then("matching", anyOf(conditionMatch, constantMatch, defaultMatch))
    .then(ignorables.ignore())
    .then("body", caseCodeBlockBody.or(expressionBlockBody))
    .display("case")

internal val switchStatement = first("keyword", keyword("switch"))
    .then(ignore(token(DefaultToken.WHITESPACE)))
    .then(token(DefaultToken.BRACKET_OPEN))
    .then(ignore(ignorables))
    .then("item", expression)
    .then(ignore(ignorables))
    .then(token(DefaultToken.BRACKET_CLOSED))
    .then(ignorables)
    .then(DefaultToken.ROUND_BRACKET_OPEN)
    .then(ignorables)
    .then("cases", case.then(ignorables).repeat())
    .then(DefaultToken.ROUND_BRACKET_CLOSED)
    .named(StatementType.SWITCH_STATEMENT)
    .display("switch statement")