package eu.nitok.jitsu.compiler.parser.jainparse

import com.niton.parser.grammar.api.Grammar.*
import com.niton.parser.grammar.api.GrammarName
import com.niton.parser.token.DefaultToken.*
import eu.nitok.jitsu.compiler.ast.CodeBlockContentType
import eu.nitok.jitsu.compiler.ast.ExpressionType.STATEMENT_EXPRESSION
import eu.nitok.jitsu.compiler.ast.StatementType.*
import eu.nitok.jitsu.compiler.parser.jainparse.AssignmentTargetType.PROPERTY_ASSIGNMENT
import eu.nitok.jitsu.compiler.parser.jainparse.AssignmentTargetType.VARIABLE_ASSIGNMENT
import eu.nitok.jitsu.compiler.parser.jainparse.matchers.ListGrammar

private val assignmentOperator =
    token(EQUAL)
        .then(ignorables)
        .then("expression", expression)
        .display("assignment")
internal val typeDeclaration = token(COLON)
    .then(ignorables)
    .then("type", type)
    .display("type declaration (: type)")

internal val typeDefinition = first("keyword", keyword("type"))
    .then(ignorables)
    .then("name", identifier)
    .then(ignorables)
    .then(token(EQUAL))
    .then(ignorables)
    .then("type", type)
    .named(TYPE_DEFINITION)
    .display("type definition")
private val variableDeclaration =
    first("keyword", (keyword("var").or(keyword("const"))))
        .then(ignorables)
        .then("name", identifier)
        .then("type_def", ignorables.ignore().then(typeDeclaration).optional().named("APPENDED_TYPE_DEF"))
        .then(ignorables.ignore())
        .then("assignment", assignmentOperator.optional())
        .named(VARIABLE_DECLARATION)
        .display("variable delcaration")

enum class AssignmentTargetType : GrammarName {
    VARIABLE_ASSIGNMENT,
    PROPERTY_ASSIGNMENT;

    override fun getName(): String {
        return this.name
    }
}

private var assignment =
    first(
        "variable",
        anyOf(reference(fieldAccess.name).namedCopy(PROPERTY_ASSIGNMENT), identifier.namedCopy(VARIABLE_ASSIGNMENT))
    )
        .then(WHITESPACE)
        .then("assignment", assignmentOperator)
        .named(ASSIGNMENT)
        .display("assignment")

private val parameter = first("name", identifier)
    .then(ignorables.ignore())
    .then("type_def", typeDeclaration)
    .then(ignorables.ignore())
    .then("default_value", assignmentOperator.optional())
    .display("parameter")

private val functionDeclaration = first("keyword", keyword("fn"))
    .then(WHITESPACE)
    .then("name", identifier.optional())
    .then(token(BRACKET_OPEN))
    .then("parameters", ListGrammar(parameter, token(COMMA).then(ignorables)))
    .then(token(BRACKET_CLOSED))
    .then(ignorables.ignore())
    .then("return_type", typeDeclaration.optional())
    .then(ignorables)
    .then("body", reference(CODE_BLOCK))
    .named(FUNCTION_DECLARATION)
    .display("function")

private val functionCall = first("function", identifier)
    .then(token(BRACKET_OPEN))
    .then("parameters", ListGrammar(expression, token(COMMA).then(ignorables)))
    .then(token(BRACKET_CLOSED))
    .named(FUNCTION_CALL)
    .display("function call")

private val returnStatement = first("keyword", keyword("return"))
    .then(WHITESPACE)
    .then("value", expression.optional())
    .named(RETURN_STATEMENT)
    .display("return")

private val yieldStatement = first("keyword", keyword("yield"))
    .then(WHITESPACE)
    .then(expression)
    .named(YIELD_STATEMENT)
    .display("yield")

private var elseStatement = first("keyword", keyword("else"))
    .then(ignorables)
    .then("code", reference(CODE_BLOCK).or(reference(IF_STATEMENT)))
    .then(ignorables)
    .display("else")

private val ifStatement = first("keyword", keyword("if"))
    .then(ignore(token(WHITESPACE)))
    .then(token(BRACKET_OPEN))
    .then(ignore(ignorables))
    .then("condition", expression)
    .then(ignore(ignorables))
    .then(token(BRACKET_CLOSED))
    .then(ignorables)
    .then("code", reference(CODE_BLOCK))
    .then(ignorables.ignore())
    .then("else", optional(elseStatement))
    .named(IF_STATEMENT)
    .display("if statement")

internal val codeBlock = token(ROUND_BRACKET_OPEN)
    .then(ignorables.ignore())
    .then("code", reference(CodeBlockContentType.STATEMENTS).or(expression.namedCopy(CodeBlockContentType.EXPRESSION)))
    .then(ignorables.ignore())
    .then(token(ROUND_BRACKET_CLOSED))
    .named(CODE_BLOCK)
    .display("code block")

internal val methodInvocation = first("target", expression)
    .then(ignorables.ignore())
    .then(token(DOT))
    .then("method", identifier)
    .then(token(BRACKET_OPEN))
    .then("parameters", ListGrammar(expression, token(COMMA).then(ignorables)))
    .then(token(BRACKET_CLOSED))
    .setLeftRecursive(true)
    .named(METHOD_INVOCATION)
    .display("method invocation")

internal const val ATOMIC_STATEMENT_NAME = "ATOMIC_STATEMENT"
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
    typeDefinition,
    codeBlock,
    functionDeclaration,
    switchStatement
)
internal val expressionStatements = anyOf(
    methodInvocation,
    functionCall,
    ifStatement,
    codeBlock,
    functionDeclaration,
    switchStatement
).named(STATEMENT_EXPRESSION)

internal const val STATEMENT_NAME = "STATEMENT"
internal val statement = anyOf(
    *singleLineStatement,
    *blockStatement
).named(STATEMENT_NAME)

internal val invocationStatement = anyOf(
    anyOf(*singleLineStatement.map { it.then(token(SEMICOLON)).named(it.name + "_WITH_SEMICOLON") }
        .toTypedArray()).named(SEMICOLON_STATEMENT),
    *blockStatement
)

internal val codeLines = ignorables.ignore().then(invocationStatement.then(ignorables.ignore()).repeat(1))
    .named(CodeBlockContentType.STATEMENTS)
