package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located

/**
 * A instance of an [AttributeDefinition]
 */
interface Attribute : Element, Access<AttributeDefinition>{
    val properties: List<Property>
    val attachedTo: HasAttributes?

    fun getPropertyValue(propertyName: String): Expression? {
        return properties.firstOrNull { it.name.value == propertyName }?.value
    }

    interface Property : Element {
        val name: Located<String>
        val value: Expression
    }
}