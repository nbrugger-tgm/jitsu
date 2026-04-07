package eu.nitok.jitsu.parser.parsers

import com.niton.jainparse.token.DefaultToken
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.parser.ast.StatementNode
import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.CodeBlockNode.*
import eu.nitok.jitsu.parser.ast.withMessages
import eu.nitok.jitsu.common.CompilerMessage.Hint
import eu.nitok.jitsu.parser.*

fun parseCodeBlock(tokens: Tokens): StatementsCodeBlock? {
    val openKw = tokens.attempt(DefaultToken.ROUND_BRACKET_OPEN)?.location ?: return null
    val lst = mutableListOf<StatementNode>()
    val messages = CompilerMessages()
    parseStatements(tokens, lst, messages::error)
    tokens.skipWhitespace()
    val closeKw = tokens.attempt(DefaultToken.ROUND_BRACKET_CLOSED)?.location
    if(closeKw == null){
        messages.error("Unclosed code block, expected '}'", tokens.location.toRange(), Hint(
            "Code block opened here", openKw
        ))
    }
    val node = StatementsCodeBlock(lst, openKw.rangeTo(
        closeKw?: lst.lastOrNull()?.location ?: openKw
    ))
    return node.withMessages(messages)
}
