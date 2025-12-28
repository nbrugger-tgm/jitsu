package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.graph.ReasonedBoolean
import eu.nitok.jitsu.compiler.model.Walkable
import eu.nitok.jitsu.compiler.parser.Range
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
    messages.apply(this)
    return this;
}

@Serializable
sealed class AstNodeImpl : AstNode {
    override val warnings: MutableList<CompilerMessage> = mutableListOf()
    override val errors: MutableList<CompilerMessage> = mutableListOf()
}

@Serializable
data class CompilerMessages(
    val warnings: MutableList<CompilerMessage> = mutableListOf(),
    val errors: MutableList<CompilerMessage> = mutableListOf()
) {
    fun warn(warning: CompilerMessage) = warnings.add(warning)
    fun warn(message: String, location: Range, vararg hints: CompilerMessage.Hint) =
        warnings.add(CompilerMessage(message, location, hints.toList()))

    fun error(error: CompilerMessage) = errors.add(error)
    fun error(message: String, location: Range, vararg hints: CompilerMessage.Hint) =
        errors.add(CompilerMessage(message, location, hints.toList()))
    fun error(message: String, location: Located<*>, vararg hints: CompilerMessage.Hint) =
        error(message, location.location, *hints)

    fun apply(node: AstNode) {
        node.warnings.addAll(warnings)
        node.errors.addAll(errors)
    }

    fun error(boolean: ReasonedBoolean, location: Range) {
        val fullMesageChain = boolean.fullMessageChain()
        this.error(CompilerMessage(fullMesageChain.first, location, fullMesageChain.second))
    }
}

@Serializable
data class Located<T>(val value: T, val location: Range) {
    inline fun <N> map(fn: (T)->N): Located<N> {
        return Located(fn(value), location)
    }
}

fun <T> T.locatedAt(location: Range): Located<T> {
    return Located(this, location)
}