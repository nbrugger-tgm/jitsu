package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Transient

internal interface ScopeProvider: Element, ScopeAware {
    @Transient val scope: Scope
    fun setScopes() {
        fun informChildren(children: List<Element>) {
            children.forEach {
                if (it is ScopeAware) it.setEnclosingScope(scope)
                if (it is ScopeProvider) it.setScopes()
                else informChildren(it.children)
            }
        }
        informChildren(children)
    }

    override fun setEnclosingScope(parent: Scope) {
        scope.parent = parent
    }
}