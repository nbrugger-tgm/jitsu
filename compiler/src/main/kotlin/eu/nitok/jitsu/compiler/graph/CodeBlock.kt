package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
class CodeBlock(val instructions: List<Instruction>) : Element {
    override val children: List<Element> get() = instructions
}