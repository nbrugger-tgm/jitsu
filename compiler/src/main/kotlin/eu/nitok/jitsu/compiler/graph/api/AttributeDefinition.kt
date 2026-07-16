package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located

/**
 * An attribute can be attached to [TypeDefinition]s and [Function]s
 * to provide additional information about them.
 *
 * Attributes can be used to modify the behavior of the compiler
 * or to provide additional metadata about the element they are attached to.
 */
interface AttributeDefinition : Element, Accessible<AttributeDefinition> {
    override val name: Located<String>
    val properties: List<Property>

    interface Property : Element {
        val name: Located<String>
        val type: Type
    }
}