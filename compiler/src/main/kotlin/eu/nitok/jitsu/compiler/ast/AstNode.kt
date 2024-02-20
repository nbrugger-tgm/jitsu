package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.parser.Locatable
import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Serializable

interface AstNode {
    val location: Range
    val warnings: MutableList<CompilerMessage>
    val errors: MutableList<CompilerMessage>

    fun warning(warning: CompilerMessage) {
        warnings.add(warning)
    }

    fun error(error: CompilerMessage) {
        warnings.add(error)
    }
}

/**
 * A node that can be an error
 */
@Serializable
abstract class AstNodeImpl : AstNode {
    override val warnings: MutableList<CompilerMessage> = mutableListOf()
    override val errors: MutableList<CompilerMessage> = mutableListOf()
}

data class CompilerMessages(
    val warnings: MutableList<CompilerMessage> = mutableListOf(),
    val errors: MutableList<CompilerMessage> = mutableListOf()
) {
    fun warn(warning: CompilerMessage) = warnings.add(warning)
    fun warn(message: String, location: Locatable, hints: List<CompilerMessage.Hint> = emptyList()) =
        warnings.add(CompilerMessage(message, location, hints))

    fun error(error: CompilerMessage) = errors.add(error)
    fun error(message: String, location: Locatable, hints: List<CompilerMessage.Hint> = emptyList()) =
        errors.add(CompilerMessage(message, location, hints))

    fun apply(node: AstNodeImpl) {
        node.warnings.addAll(warnings)
        node.errors.addAll(errors)
    }
}

interface Located<T> {
    val location: Range;
    val value: T;
}