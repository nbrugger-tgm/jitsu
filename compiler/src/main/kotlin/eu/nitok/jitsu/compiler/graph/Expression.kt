package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
sealed interface Expression {
    val isConstant: Boolean;

    @Serializable
    data object Undefined: Expression {
        override val isConstant: Boolean get() = false
    }
}