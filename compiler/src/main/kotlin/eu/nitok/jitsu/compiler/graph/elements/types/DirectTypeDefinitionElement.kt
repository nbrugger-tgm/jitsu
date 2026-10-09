package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.compiler.graph.api.Access
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.DirectTypeDefinition
import eu.nitok.jitsu.compiler.graph.elements.AccessibleElement
import eu.nitok.jitsu.compiler.graph.elements.JitsuModule
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.Int

@Serializable
internal sealed class DirectTypeDefinitionElement : TypeElement(), TypeDefinitionElement,
    AccessibleElement<DirectTypeDefinition> {
    override fun getSymbol(module: JitsuModule) = module.getSymbolID(this)
    @Transient override lateinit var module: JitsuModule
    override var symbolIndex: Int? = null

    @Transient
    override val accessToSelf: MutableList<Access<DirectTypeDefinition>> = mutableListOf()
    @Transient
    override val asTypeDefinition: DirectTypeDefinition = when(this) {
        is DirectTypeDefinition -> this
        is Enum -> this
        is TypeParameterElement -> this
    }
}
