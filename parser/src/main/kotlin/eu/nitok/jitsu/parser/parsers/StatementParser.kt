package eu.nitok.jitsu.parser.parsers

import com.niton.jainparse.token.DefaultToken
import eu.nitok.jitsu.parser.ast.*
import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.parser.*


/**
 * Parses any statement from the token stream.
 * Handles function declarations, class declarations, variable declarations, return statements,
 * type declarations, assignments, and function calls.
 *
 * @return The parsed statement node, or null if no valid statement starts at the current position.
 */
fun parseStatement(tokens: Tokens): StatementNode? {
    return parseFunction(tokens) ?: parseClass(tokens)?: parseExecutableStatement(tokens) {
        parseVariableDeclaration(it) ?: parseReturnStatement(it) ?: parseTypeDeclaration(tokens)
        ?: parseIdentifierBased(it) { tokens, id ->
            parseAssignment(tokens, id) ?: parseFunctionCall(tokens, id)
        }
    }
}

/**
 * Parses an assignment statement after an identifier has been parsed.
 *
 * Example: `= 5` after identifier `x` produces `x = 5`
 *
 * @param kw The already-parsed target identifier.
 * @return An AssignmentNode, or null if no `=` follows the identifier.
 */
fun parseAssignment(tokens: Tokens, kw: IdentifierNode): StatementNode.InstructionNode.AssignmentNode? {
    tokens.skipWhitespace()
    tokens.attempt(DefaultToken.EQUAL) ?: return null
    tokens.skipWhitespace()
    val expression = parseExpression(tokens)
    return StatementNode.InstructionNode.AssignmentNode(ExpressionNode.VariableReferenceNode(kw), expression).run {
        if (expression == null)
            this.error(CompilerMessage("Expected value to assign to '${kw.value}'", tokens.location.toRange()))
        this
    }
}

/**
 * Parses multiple statements from the token stream until exhausted or an unrecoverable error.
 *
 * @param statements Accumulator list for parsed statements.
 * @param containerNode Error callback for invalid input between statements.
 */
fun parseStatements(
    tokens: Tokens,
    statements: MutableList<StatementNode>,
    containerNode: (CompilerMessage) -> Unit
) {
    while (tokens.hasNext()) {
        tokens.skipWhitespace()
        when (val x = parseStatement(tokens)) {
            is StatementNode -> statements.add(x)
            null -> {
                tokens.skipWhitespace()
                val lastToken = tokens.index()
                val invalid = tokens.skipUntil(
                    DefaultToken.ROUND_BRACKET_CLOSED,
                    DefaultToken.SEMICOLON,
                    DefaultToken.NEW_LINE
                )
                if (lastToken == tokens.index()) {
                    break
                }
                tokens.skip(DefaultToken.SEMICOLON)
                containerNode(CompilerMessage("Expected a statement", invalid))
            }
        }
    }
}


private fun parseExecutableStatement(tokens: Tokens, statmentFn: (Tokens) -> StatementNode?): StatementNode? {
    val res = statmentFn(tokens) ?: return null
    val endOfStatementPos = tokens.location.toRange()
    tokens.skipWhitespace()
    val semicolon = tokens.peekOptional()
    if (semicolon.map { it.type }.orElse(null) != DefaultToken.SEMICOLON) {
        res.error(CompilerMessage("Expect semicolon at end of statement!", endOfStatementPos))
    } else {
    }
    tokens.skip()
    return res
}

/**
 * Parses a function call after an identifier has been parsed.
 *
 * Example: `(1, 2)` after identifier `foo` produces `foo(1, 2)`
 *
 * @param id The already-parsed function name identifier.
 * @return A FunctionCallNode, or null if no `(` follows the identifier.
 */
fun parseFunctionCall(tokens: Tokens, id: IdentifierNode): StatementNode.InstructionNode.FunctionCallNode? {
    val messages = CompilerMessages()
    val params = tokens.range {
        enclosedRepetition(
            DefaultToken.BRACKET_OPEN,
            DefaultToken.COMMA,
            DefaultToken.BRACKET_CLOSED,
            messages,
            "parameter list",
            "parameter"
        ) { parseExpression(it) }
    }
    val paramList = params.value ?: return null
    return StatementNode.InstructionNode.FunctionCallNode(id, paramList, id.location.rangeTo(params.location))
        .withMessages(messages)
}

/**
 * Parses a return statement starting with the `return` keyword.
 *
 * Examples: `return`, `return 42`, `return someVar`
 *
 * @return A ReturnNode, or null if no `return` keyword is present.
 */
fun parseReturnStatement(tokens: Tokens): StatementNode? {
    val kw = tokens.keyword("return") ?: return null
    tokens.skipWhitespace()
    val value = parseExpression(tokens)
    return StatementNode.InstructionNode.ReturnNode(value, kw.rangeTo(value?.location ?: kw), kw)
}

/**
 * Parses a variable declaration starting with the `var` keyword.
 *
 * Syntax: `var <name>[: <type>] = <expression>`
 *
 * Examples: `var x = 5`, `var x: i32 = 5`, `var items: Array<i64> = arr`
 *
 * @return A VariableDeclarationNode, or null if no `var` keyword is present.
 */
fun parseVariableDeclaration(tokens: Tokens): StatementNode.InstructionNode.VariableDeclarationNode? {
    val kw = tokens.keyword("var") ?: return null
    val messages = CompilerMessages()
    tokens.skipWhitespace()
    val name = parseIdentifier(tokens)
    if (name == null) {
        messages.error("Expected variable name", tokens.location.toRange())
        val invalid = tokens.skipUntil(DefaultToken.SEMICOLON, DefaultToken.NEW_LINE, DefaultToken.EQUAL, DefaultToken.COLON)
    } else {
        tokens.skipWhitespace()
    }
    val type = parseExplicitType(tokens, messages)
    tokens.skipWhitespace()
    val eq = tokens.attempt(DefaultToken.EQUAL)
    if (eq == null) {
        messages.error("Variables need an initial value!", tokens.location.toRange())
    } else {
        tokens.skipWhitespace()
    }
    val expression = parseExpression(tokens)

    if (expression == null)
        messages.error("Expected value to assign to '${name?.value}'", tokens.location.toRange())
    return StatementNode.InstructionNode.VariableDeclarationNode(
        name,
        type,
        expression,
        kw
    ).withMessages(messages)
}
