package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Location
import kotlinx.serialization.Serializable

@Serializable
sealed interface Instruction : Element {
    interface Return : Instruction {
        val value: Expression?
        val location: Location
    }
    interface FunctionCall : Instruction, Expression, Access.FunctionAccess {
        val callParameters: List<Expression>
        val parameters: Map<String, Expression>
    }
}