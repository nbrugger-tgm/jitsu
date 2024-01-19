package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.diagnostic.CompilerError
import kotlinx.serialization.Serializable

interface AstNode {
    val location: Location
    val warnings: MutableList<CompilerError>
    val errors: MutableList<CompilerError>

    fun warning(warning: CompilerError) {
        warnings.add(warning)
    }

    fun error(error: CompilerError) {
        warnings.add(error)
    }
}

/**
 * A node that can be an error
 */
@Serializable
abstract class AstNodeImpl : AstNode {
    override val warnings: MutableList<CompilerError> = mutableListOf()
    override val errors: MutableList<CompilerError> = mutableListOf()
}

typealias Location = @Serializable(with = LocationSerializer::class) com.niton.parser.token.Location
interface Located<T> {
    val location: Location;
    val value: T;
}