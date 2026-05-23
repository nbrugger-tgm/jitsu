package eu.nitok.jitsu.compiler.graph.api

import java.net.URI

interface JitsuFile : Element {
    val imports: List<Import>
    val functions: List<Function>
    val variables: List<Variable>
    val types: List<TypeDefinition>
    val uri: URI

    val scope: Scope
    val module: JitsuModule
}