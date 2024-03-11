package eu.nitok.jitsu.compiler.parser.parsers

import com.niton.jainparse.token.DefaultToken
import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.StatementNode
import eu.nitok.jitsu.compiler.ast.StatementNode.InstructionNode.CodeBlockNode.*
import eu.nitok.jitsu.compiler.ast.withMessages
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage.Hint
import eu.nitok.jitsu.compiler.parser.*
import kotlin.jvm.optionals.getOrNull

fun parseCodeBlock(tokens: Tokens): StatementsCodeBlock? {
    val openKw = tokens.expect(DefaultToken.ROUND_BRACKET_OPEN)?.location ?: return null;
    val lst = mutableListOf<StatementNode>()
    val messages = CompilerMessages()
    parseStatements(tokens, lst, messages::error)
    tokens.skipWhitespace()
    val closeKw = tokens.expect(DefaultToken.ROUND_BRACKET_CLOSED)?.location
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