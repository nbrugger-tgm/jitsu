package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located
import kotlinx.serialization.Serializable

@Serializable
sealed interface Variable : Accessible<Variable>, Element {
    override val name: Located<String>
    val reassignable: Boolean
    val declaredType: Type?

    /**
     * the type for this variable. [declaredType] if present, else the implicit type from initial value
     */
    val type: Type
    val initialValue: Expression?
}