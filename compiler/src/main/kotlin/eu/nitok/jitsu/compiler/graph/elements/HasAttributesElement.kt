package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.compiler.graph.api.HasAttributes

internal interface HasAttributesElement {
    val asHasAttributes: HasAttributes get() = when(this) {
        is HasAttributes -> this
        else -> error("$this implements HasAttributesElement but is not a HasAttributes")
    }
    val attributes: List<AttributeElement>
}