package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
sealed interface Expression {
    @Serializable
    data object Undefined: Expression
}