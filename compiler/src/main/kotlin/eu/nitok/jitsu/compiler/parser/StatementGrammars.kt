package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.model.CodeBlockContent
import eu.nitok.jitsu.compiler.model.StatementType.*
import eu.nitok.jitsu.compiler.parser.matchers.ListGrammar

private val assignmentOperator =
    token(EQUAL)
        .then(ignorables)
        .then("expression", expression)
val typeDeclaration = token(COLON)
    .then(ignorables)
    .then("type", type)
private val variableDeclaration =
    (keyword("var").or(keyword("const")))
        .then(ignorables)
        .then("name", identifier)
        .then("type_def", ignorables.ignore().then(typeDeclaration).optional().named("APPENDED_TYPE_DEF"))
        .then(ignorables.ignore())
        .then("assignment", assignmentOperator.optional())
        .named(VARIABLE_DECLARATION)

private var assignment = first("variable", identifier)
    .then(WHITESPACE)
    .then("assignment", assignmentOperator)
    .named(ASSIGNMENT)

private val functionDeclaration = keyword("fn")
    .then(WHITESPACE)
    .then("name", identifier.optional())
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

val methodInvocation = first("target", anyOf(*atomicExpressions))
    .then(token(POINT))
    .then("method", identifier)
    .then(token(BRACKET_OPEN))
    .then("parameters", ListGrammar(expression, token(COMMA).then(ignorables)))
    .then(token(BRACKET_CLOSED))
    .setLeftRecursive(true)
    .named(METHOD_INVOCATION)

const val ATOMIC_STATEMENT_NAME = "ATOMIC_STATEMENT"
private val atomicStatement = anyOf(
    functionCall,
    methodInvocation
).named(ATOMIC_STATEMENT_NAME);

private var singleLineStatement = arrayOf(
    variableDeclaration,
    returnStatement,
    yieldStatement,
    methodInvocation,
    functionCall,
    assignment
)

private val blockStatement = arrayOf(
    ifStatement,
    codeBlock,
    functionDeclaration,
    switchStatement
)

const val STATEMENT_NAME = "STATEMENT"
val statement = anyOf(
    *blockStatement,
    *singleLineStatement
).named(STATEMENT_NAME)

val invocationStatement = anyOf(
    *blockStatement,
    anyOf(*singleLineStatement.map { it.then(token(SEMICOLON)).named(it.name+"_WITH_ARRAY") }.toTypedArray()).named(STATEMENT_WITH_SEMICOLON)
)

val codeLines = invocationStatement.or(ignorables.ignore()).repeat(1).named(CodeBlockContent.STATEMENTS)
