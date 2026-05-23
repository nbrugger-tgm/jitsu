package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Instruction
import kotlinx.serialization.Serializable

@Serializable
sealed interface InstructionElement : Element {
    val asInstruction: Instruction
        get() = when (this) {
            is Return -> this
            is FunctionElement -> this
            is VariableDeclaration -> this
            is FunctionCall -> this
        }
}