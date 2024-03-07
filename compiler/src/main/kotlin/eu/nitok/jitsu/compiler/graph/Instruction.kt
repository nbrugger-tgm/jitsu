package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Instruction : Element {
    @Serializable
    data class Return(val value: Expression?): Instruction(), FunctionAware {
        override val children: List<Element> get() = listOfNotNull(value)
        @Transient override lateinit var function: Function
    }

    @Serializable
    data class VariableDeclaration(val variable: Variable, val value: Expression): Instruction() {
        override val children: List<Element> get() = listOf(variable, value)
    }
}