package eu.nitok.jitsu.compiler.graph.behaviour

import eu.nitok.jitsu.compiler.graph.elements.Scope

internal interface ScopeAware {
    fun setEnclosingScope(parent: Scope)
}
