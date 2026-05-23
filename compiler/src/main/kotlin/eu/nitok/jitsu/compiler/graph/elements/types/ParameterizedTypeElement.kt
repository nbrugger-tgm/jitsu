package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.compiler.graph.api.Access
import eu.nitok.jitsu.compiler.graph.api.Accessible
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.ParameterizedType
import eu.nitok.jitsu.compiler.graph.elements.AccessElement
import eu.nitok.jitsu.compiler.graph.elements.AccessibleElement
import eu.nitok.jitsu.compiler.graph.elements.JitsuModule
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.Int
import kotlin.properties.Delegates

@Serializable
internal sealed class ParameterizedTypeElement: AccessibleElement, TypeDefinitionElement {
    @Transient
    override val accessToSelf: MutableList<Access<TypeDefinition>> = mutableListOf()


    abstract val generics: List<TypeParameterElement>

    @Transient
    override val asTypeDefinition: ParameterizedType = when(this) {
        is ParameterizedType -> this
        is Class -> this
        is Interface -> this
        is Struct -> this
        is TypeAlias -> this
    }
    abstract fun toType(typeParameters: Map<String, TypeElement>): TypeElement

    override fun getSymbol(module: JitsuModule) = module.getSymbolID(this)
    override var symbolIndex by Delegates.notNull<Int>()
    @Transient override lateinit var module: JitsuModule

}