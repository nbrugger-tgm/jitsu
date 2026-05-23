package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located
import kotlinx.serialization.Serializable

@Serializable
sealed interface Variable : Accessible<Variable>, Element {
    override val name: Located<String>
    val reassignable: Boolean
    val declaredType: Type?
    val initialValue: Expression?
}