package eu.nitok.jitsu.compiler.graph.api

interface VariableDeclaration : Variable, Instruction {
    val implicitType: Type?
}