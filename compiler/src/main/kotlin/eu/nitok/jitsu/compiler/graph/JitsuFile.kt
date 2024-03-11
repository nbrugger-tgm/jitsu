package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.CompilerMessages
import kotlinx.serialization.Serializable

@Serializable
class JitsuFile(override val scope: Scope, val messages: CompilerMessages): ScopeProvider {
    override val children: List<Element> get() = scope.elements.toList()
}