package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Access
import eu.nitok.jitsu.compiler.graph.api.AttributeDefinition
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.elements.types.Null
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal class AttributeDefinitionElement(
    override val name: Located<String>,
    override val properties: List<Property>
): AttributeDefinition, AccessibleElement<AttributeDefinition> {

    override fun getSymbol(module: JitsuModule) = module.getSymbolID(this)
    override var symbolIndex: Int? = null
    @Transient override lateinit var module: JitsuModule

    override val children: List<Element>
        get() = properties

    @Transient
    override val accessToSelf: MutableList<Access<AttributeDefinition>> = mutableListOf()

    @Serializable
    class Property(
        override val name: Located<String>,
        val typeElement: TypeElement
    ) : AttributeDefinition.Property {
        @Transient
        override val type: Type = typeElement.asType
        @Transient
        override val children: List<Element> = listOf(typeElement)

        val isRequired = !typeElement.acceptsInstanceOf(Null).value
    }
}