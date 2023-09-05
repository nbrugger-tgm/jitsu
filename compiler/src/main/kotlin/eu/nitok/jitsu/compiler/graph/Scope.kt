package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.ExpressionNode
import eu.nitok.jitsu.compiler.ast.Location
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Content of a file or everything that is between { and }
 */
@Serializable
class Scope(val parent: Scope?) {
    val contants: MutableList<Constant<@Contextual Any>> = mutableListOf();
    val types: MutableMap<String, ResolvedType> = mutableMapOf()
    val functions: MutableList<Function> = mutableListOf()
    val variable: MutableList<Variable> = mutableListOf()
    val errors: MutableList<Error> = mutableListOf()
    @Serializable
    data class Error(val message: String, val location: Location)
}
