package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
class CodeBlock(val instructions: List<Instruction>, override val scope: Scope) : Element, ScopeAware, ScopeProvider {
    override val children: List<Element> get() = instructions

    override fun setEnclosingScope(parent: Scope) {
        scope.parent = parent
    }
}