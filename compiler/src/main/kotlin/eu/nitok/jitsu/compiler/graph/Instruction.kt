package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
sealed interface Instruction {
    @Serializable
    data class VariableDeclaration(val variable: Variable, val value: Expression): Instruction
}