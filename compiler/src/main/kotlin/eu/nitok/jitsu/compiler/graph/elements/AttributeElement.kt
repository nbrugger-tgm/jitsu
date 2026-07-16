package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Attribute
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.api.HasAttributes
import eu.nitok.jitsu.compiler.graph.behaviour.Finalizable
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal class AttributeElement(
    override val properties: List<Property>,
    val access:AccessElement.AttributeAccessElement
) : AccessElement.AttributeAccess by access, Attribute, ModuleAware, Finalizable {

    constructor(
        access: Located<String>,
        properties: List<Property>
    ) : this(properties, AccessElement.AttributeAccessElement(access))

    fun attachTo(parent: HasAttributesElement) {
        attachedTo = parent.asHasAttributes
    }
    @Transient
    override var attachedTo: HasAttributes? = null
        private set
    override val children: List<Element>
        get() = properties

    override fun finalize(messages: CompilerMessages) {
        verifyProperties(messages)
    }

    private fun verifyProperties(messages: CompilerMessages) {
        val target = targetElement ?: return
        val duplicates = mutableListOf<Pair<Property, Property>>()
        val unknown = mutableSetOf<Property>()
        val matching = mutableMapOf<String, Property>()

        properties.forEach { property ->
            val declaration = target.properties.find { it.name.value == property.name.value }
            if(matching.contains(property.name.value)) duplicates.add(matching[property.name.value]!! to property)
            if(declaration != null) {
                matching[property.name.value] = property
            } else unknown.add(property)
        }
        val missing = target.properties.filter { it.isRequired }.filter { matching[it.name.value] == null }

        missing.forEach { missing ->
            messages.error("Property '${missing.name.value}' is not nullable/required but is not provided", this.reference,
                CompilerMessage.Hint("Property is provided here", missing.name)
            )
        }
        unknown.forEach { unknown ->
            val remaining = target.properties.filter { matching.containsKey(it.name.value) }
            messages.error(
                "Attribute ${target.name.value} has no property named ${unknown.name.value}. ${if(remaining.isNotEmpty()) "Remaining properties: $remaining" else ""}",
                unknown.name.location,
                CompilerMessage.Hint("${target.name.value} is defined here", target.name.location)
            )
        }
        duplicates.forEach { (first, second) ->
            messages.error(
                "${first.name.value} is already provided", second.name.location,
                CompilerMessage.Hint("First provided here", first.name.location)
            )
        }
        matching.forEach { (name, property) ->
            val target = target.properties.find { it.name.value == name }!!
            val typeMatch = target.type.acceptsInstanceOf(property.value.type)
            if(!typeMatch.value) {
                messages.error(typeMatch, property.valueElement.location)
            }
        }
    }


    @Serializable
    internal data class Property(
        override val name: Located<String>,
        val valueElement: ExpressionElement
    ) : Attribute.Property {
        override val value: Expression get() = valueElement.asExpression
        @Transient
        override val children: List<Element> = listOf(value)
    }
}
