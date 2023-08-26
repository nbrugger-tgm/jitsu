package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*
import com.niton.parser.token.ListTokenStream
import eu.nitok.jitsu.compiler.model.CodeBlockContent
import eu.nitok.jitsu.compiler.model.StatementType
import eu.nitok.jitsu.compiler.model.StatementType.*
import eu.nitok.jitsu.compiler.parser.matchers.ListGrammar

private val assignmentOperator =
    token(EQUAL)
        .then(ignorables)
        .then("expression", expression)

private val variableDeclaration =
    (keyword("var").or(keyword("const")))
        .then(WHITESPACE)
        .then("name", identifier)
        .then("type_def", token(WHITESPACE).then(TYPE_DEF).optional())
        .then(token(WHITESPACE).ignore())
        .then("assignment", assignmentOperator.optional())
        .named(VARIABLE_DECLARATION)

private var assignment = first("variable", identifier)
    .then(WHITESPACE)
    .then("assignment", assignmentOperator)
    .named(ASSIGNMENT)

private val functionDeclaration = keyword("fn")
    .then(WHITESPACE)
    .then("name", identifier)
    .then(token(BRACKET_OPEN))
    .then(token(BRACKET_CLOSED))
    .then(token(WHITESPACE).ignore())
    .then("body", reference(CODE_BLOCK))
    .named(FUNCTION_DECLARATION)

private val functionCall = first("function", identifier)
    .then(token(BRACKET_OPEN))
    .then("parameters", ListGrammar(expression, token(COMMA).then(ignorables)))
    .then(token(BRACKET_CLOSED))
    .named(FUNCTION_CALL)
val methodInvocation = first("target", expression)
    .then(token(POINT))
    .then("method", identifier)
    .then(token(BRACKET_OPEN))
    .then("parameters", ListGrammar(expression, token(COMMA).then(ignorables)))
    .then(token(BRACKET_CLOSED))
    .named(METHOD_INVOCATION)

private val returnStatement = keyword("return")
    .then(WHITESPACE)
    .then(expression)
    .named(RETURN_STATEMENT)

private val yieldStatement = keyword("yield")
    .then(WHITESPACE)
    .then(expression)
    .named(YIELD_STATEMENT)

private var elseStatement = keyword("else")
    .then(ignorables)
    .then("code", reference(CODE_BLOCK))
    .then(ignorables)

private val ifStatement = keyword("if")
    .then(ignore(token(WHITESPACE)))
    .then(token(BRACKET_OPEN))
    .then(ignore(ignorables))
    .then("condition", expression)
    .then(ignore(ignorables))
    .then(token(BRACKET_CLOSED))
    .then(ignorables)
    .then("code", reference(CODE_BLOCK))
    .then("else", optional(elseStatement))
    .named(IF_STATEMENT)

val codeBlock = token(ROUND_BRACKET_OPEN)
    .then(ignorables.ignore())
    .then("code",reference(CodeBlockContent.STATEMENTS).or(expression.namedCopy(CodeBlockContent.EXPRESSION)))
    .then(ignorables.ignore())
    .then(token(ROUND_BRACKET_CLOSED))
    .named(CODE_BLOCK);

private var atomicStatement = arrayOf(
    variableDeclaration,
    returnStatement,
    yieldStatement,
    methodInvocation,
    functionCall,
    assignment
)

private val nonAtomicStatement = arrayOf(
    ifStatement,
    codeBlock,
    functionDeclaration,
    switchStatement
)

const val STATEMENT_NAME = "STATEMENT"
val statement = anyOf(
    *nonAtomicStatement,
    *atomicStatement
).named(STATEMENT_NAME)

val invocationStatement = anyOf(
    *nonAtomicStatement,
    *atomicStatement.map { it.then(token(SEMICOLON)).named("${it.name} with semicolon") }.toTypedArray()
)

val codeLines = invocationStatement.or(ignorables.ignore()).repeat(1).named(CodeBlockContent.STATEMENTS)
