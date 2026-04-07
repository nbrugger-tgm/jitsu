package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Walkable
import eu.nitok.jitsu.common.Range
import kotlinx.serialization.Serializable

@Serializable
sealed interface AstNode : Walkable<AstNode> {
    val location: Range
    val warnings: MutableList<CompilerMessage>
    val errors: MutableList<CompilerMessage>

    fun warning(warning: CompilerMessage) {
        warnings.add(warning)
    }

    fun error(error: CompilerMessage) {
        errors.add(error)
    }
}
fun <T: AstNode> T.withMessages(messages: CompilerMessages) : T{
    messages.applyTo(this)
    return this;
}

fun CompilerMessages.applyTo(node: AstNode) {
    node.warnings.addAll(warnings)
    node.errors.addAll(errors)
}

@Serializable
sealed class AstNodeImpl : AstNode {
    override val warnings: MutableList<CompilerMessage> = mutableListOf()
    override val errors: MutableList<CompilerMessage> = mutableListOf()
}

