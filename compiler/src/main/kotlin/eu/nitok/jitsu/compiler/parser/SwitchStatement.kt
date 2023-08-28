package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.grammar.api.GrammarName
import com.niton.parser.token.DefaultToken
import com.niton.parser.token.DefaultToken.COLON
import com.niton.parser.token.DefaultToken.SEMICOLON
import eu.nitok.jitsu.compiler.model.CaseBodyType
import eu.nitok.jitsu.compiler.model.CaseMatchType
import eu.nitok.jitsu.compiler.model.StatementType
import eu.nitok.jitsu.compiler.parser.matchers.ListGrammar

private val constantMatch = literalExpression.namedCopy(CaseMatchType.CONSTANT_CASE);

enum class CaseMatchingPattern : GrammarName {
    DECONSTRUCT_PATTERN_MATCH, CASTING_PATTERN_MATCH;

    override fun getName(): String {
        return name
    }
}

val DECONSTRUCT_MATCH = token(DefaultToken.BRACKET_OPEN)
    .then(ignorables.ignore())
    .then("variables", ListGrammar(identifier, token(DefaultToken.COMMA).then(ignorables)))
    .then(ignorables.ignore())
    .then(token(DefaultToken.BRACKET_CLOSED))
    .named(CaseMatchingPattern.DECONSTRUCT_PATTERN_MATCH)
val TYPECAST_MATCH = identifier.namedCopy(CaseMatchingPattern.CASTING_PATTERN_MATCH)
private val typematch =
    first("type", identifier)
        .then(ignorables.ignore())
        .then("matching", DECONSTRUCT_MATCH.or(TYPECAST_MATCH))
private val conditionMatch = typematch.then(optional(expression)).named(CaseMatchType.CONDITION_CASE)
private val defaultMatch = keyword("default").named(CaseMatchType.DEFAULT_CASE);


private val caseCodeBlockBody = token(COLON).then(ignorables).then("body", codeBlock).named(CaseBodyType.CODE_BLOCK_CASE_BODY)
private val expressionBlockBody = keyword("->").then(ignorables).then("body", expression).then(token(SEMICOLON)).named(CaseBodyType.EXPRESSION_CASE_BODY)

private val case = keyword("case")
    .then(ignorables)
    .then("matching", anyOf(conditionMatch, constantMatch, defaultMatch))
    .then(ignorables.ignore())
    .then("body",caseCodeBlockBody.or(expressionBlockBody))

val switchStatement = keyword("switch")
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
    .then(ignorables)
    .then(DefaultToken.ROUND_BRACKET_CLOSED)
    .named(StatementType.SWITCH_STATEMENT)