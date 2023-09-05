package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
open class StructDefinition(
    private val fields: MutableSet<Field>,
    val embedded: MutableSet<StructDefinition> = mutableSetOf()
) {
    @Serializable
    data class Field(val name: String, var mutable: Boolean, val type: ResolvedType)

    val allFields: Set<Field> get() = embedded.flatMap { it.allFields }.toSet() + fields

}