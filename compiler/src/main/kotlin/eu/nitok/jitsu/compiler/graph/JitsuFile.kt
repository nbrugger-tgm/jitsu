package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.analysis.AnalysisRepository
import eu.nitok.jitsu.common.CompilerMessages
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class JitsuFile(override val scope: Scope, val messages: CompilerMessages): ScopeProvider {
    @Transient
    var analysisRepository: AnalysisRepository? = null
        internal set

    override val children: List<Element> get() = scope.elements.toList()
}