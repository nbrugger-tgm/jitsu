package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
sealed class Instruction : Element {
    @Serializable
    data class Return(val value: Expression?): Instruction() {
        override val children: List<Element> get() = listOfNotNull(value)
    }

    @Serializable
    data class VariableDeclaration(val variable: Variable, val value: Expression): Instruction() {
        override val children: List<Element> get() = listOf(variable, value)
    }
}