package eu.nitok.jitsu.compiler.graph.behaviour

import eu.nitok.jitsu.compiler.graph.elements.JitsuModule

internal interface ModuleAware {
    fun setEnclosingModule(parent: JitsuModule)
}
