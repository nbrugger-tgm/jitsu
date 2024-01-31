package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import kotlinx.serialization.Serializable

interface AstNode {
    val location: Location
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

typealias Location = @Serializable(with = LocationSerializer::class) eu.nitok.jitsu.compiler.parser.Location
interface Located<T> {
    val location: Location;
    val value: T;
}