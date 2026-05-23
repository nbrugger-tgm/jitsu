package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.compiler.graph.api.CodeBlock
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Instruction
import kotlinx.serialization.Serializable

@Serializable
internal class CodeBlockElement(val instructionElements: List<InstructionElement>) : CodeBlock {
    override val instructions: List<Instruction> by lazy { instructionElements.map { it.asInstruction } }
    override val children: List<Element> get() = instructions
}