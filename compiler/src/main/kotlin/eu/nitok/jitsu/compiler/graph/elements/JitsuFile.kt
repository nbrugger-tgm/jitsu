package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.compiler.graph.IrStore
import eu.nitok.jitsu.compiler.graph.SymbolID
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.JitsuFile
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeProvider
import eu.nitok.jitsu.compiler.graph.elements.types.TypeDefinitionElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.net.URI

@Serializable
internal class JitsuFile internal constructor(
    override val functions: List<FunctionElement>,
    val typeElements: List<TypeDefinitionElement>,
    val variableElements: List<VariableElement>,
    override val imports: List<Import>,
    val path: String
) : ScopeProvider, ModuleAware, JitsuFile {

    override val variables by lazy {
        variableElements.map { it.asVariable }
    }
    override val types by lazy {
        typeElements.map { it.asTypeDefinition }
    }
    override val uri by lazy { URI(path) }
    override val children: List<Element> get() = functions + types + variables + imports

    @Transient
    override val scope: Scope = Scope(imports = imports)

    @Transient
    override lateinit var module: JitsuModule
    override fun setEnclosingModule(parent: JitsuModule) {
        module = parent
    }
}