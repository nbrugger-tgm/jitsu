package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken
import eu.nitok.jitsu.compiler.ast.*
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.*


fun parseStatement(tokens: Tokens): StatementNode? {
    return parseFunction(tokens) ?: parseExecutableStatement(tokens) {
        parseVariableDeclaration(it) ?: parseReturnStatement(it) ?: parseTypeDeclaration(tokens)
        ?: parseIdentifierBased(it) { tokens, id ->
            parseAssignment(tokens, id) ?: parseFunctionCall(tokens, id)
        }
    }
}

fun parseAssignment(tokens: Tokens, kw: IdentifierNode): StatementNode.InstructionNode.AssignmentNode? {
    tokens.skipWhitespace()
    tokens.keyword(DefaultToken.EQUAL) ?: return null
    tokens.skipWhitespace()
    val expression = parseExpression(tokens)
    return StatementNode.InstructionNode.AssignmentNode(ExpressionNode.VariableReferenceNode(kw), expression).run {
        if (expression == null)
            this.error(CompilerMessage("Expected value to assign to '${kw.value}'", tokens.location.toRange()))
        this
    }
}

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
                    //nothing invalid was skipped - so end of block
                    break
                }
                tokens.skip(DefaultToken.SEMICOLON)
                containerNode(CompilerMessage("Expected a statement", invalid))
            }
        }
    }
}


/**
 * Parses statments that end with a semicolon
 */
private fun parseExecutableStatement(tokens: Tokens, statmentFn: (Tokens) -> StatementNode?): StatementNode? {
    val res = statmentFn(tokens) ?: return null
    val endOfStatementPos = tokens.location.toRange()
    tokens.skipWhitespace()
    val semicolon = tokens.peekOptional()
    if (semicolon.map { it.type }.orElse(null) != DefaultToken.SEMICOLON) {
        res.error(CompilerMessage("Expect semicolon at end of statement!", endOfStatementPos))
    } else {
        tokens.skip()
    }
    return res
}

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
    if (params.value == null) return null
    return StatementNode.InstructionNode.FunctionCallNode(id, params.value, id.location.rangeTo(params.location))
        .withMessages(messages)
}

fun parseReturnStatement(tokens: Tokens): StatementNode? {
    val kw = tokens.keyword("return") ?: return null
    tokens.skipWhitespace()
    val value = parseExpression(tokens)
    return StatementNode.InstructionNode.ReturnNode(value, kw.rangeTo(value?.location ?: kw), kw)
}

fun parseVariableDeclaration(tokens: Tokens): StatementNode.InstructionNode.VariableDeclarationNode? {
    val kw = tokens.keyword("var") ?: return null
    val messages = CompilerMessages()
    tokens.skipWhitespace()
    val name = parseIdentifier(tokens)
    tokens.skipWhitespace()
    val type = parseOptionalExplicitType(tokens, messages::error)
    tokens.skipWhitespace()
    val eq = tokens.keyword(DefaultToken.EQUAL)
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