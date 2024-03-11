package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Transient

interface ScopeAware {
    fun setEnclosingScope(parent: Scope)
}
