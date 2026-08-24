package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located


interface StructStructure {
    /**
     * Fields declared in this struct plus all fields from [embedded][embedded] Structs
     */
    val allFields: Set<Field>

    /**
     * Fields declared directly in this struct
     */
    val fields: Set<Field>

    /**
     * Structs that are embedded in this struct. Their fields are "copied" to this struct
     */
    val embedded: Set<Lazy<StructStructure>>

    interface Field : Element, Accessible<Field> {
        val mutable: Boolean
        val type: Type
        override val name: Located<String>
    }
}

interface InterfaceStructure {
    val methods: Map<String, List<NamedFunctionSignature>>
}

interface ClassStructure {
    val fields: List<StructStructure.Field>
    val methods: List<Function>
}