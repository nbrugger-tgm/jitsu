package eu.nitok.jitsu.compiler.graph.behaviour

import eu.nitok.jitsu.compiler.graph.elements.FunctionElement

internal interface FunctionAware {
    fun setEnclosingFunction(parent: FunctionElement)
}
