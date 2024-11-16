package eu.nitok.jitsu.compiler.graph

internal interface ScopeProvider: Element {
    val scope: Scope
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
}