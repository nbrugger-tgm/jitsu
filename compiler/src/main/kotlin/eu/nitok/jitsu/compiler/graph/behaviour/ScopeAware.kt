package eu.nitok.jitsu.compiler.graph.behaviour

import eu.nitok.jitsu.compiler.graph.Scope

interface ScopeAware {
    fun setEnclosingScope(parent: Scope)
}
