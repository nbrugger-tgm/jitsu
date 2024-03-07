package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Transient

interface ScopeAware {
    @Transient var scope: Scope
}
