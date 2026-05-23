package eu.nitok.jitsu.compiler.graph.api

interface CodeBlock : Element {
    val instructions: List<Instruction>
}