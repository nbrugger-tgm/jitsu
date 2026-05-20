package eu.nitok.jitsu.compiler.graph.behaviour

import eu.nitok.jitsu.compiler.graph.Function

interface FunctionAware {
    fun setEnclosingFunction(parent: Function)
}
