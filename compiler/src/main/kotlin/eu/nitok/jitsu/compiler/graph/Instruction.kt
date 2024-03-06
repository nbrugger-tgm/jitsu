package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
sealed class Instruction {
    @Serializable
    data class Return(val value: Expression?): Instruction()

    @Serializable
    data class VariableDeclaration(val variable: Variable, val value: Expression): Instruction()
}