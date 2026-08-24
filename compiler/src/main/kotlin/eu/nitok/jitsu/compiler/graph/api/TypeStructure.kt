package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.ParameterizedType.StructDefinition

sealed interface TypeStructure {
    interface Class {
        val fields: List<Struct.Field>
        val methods: List<Function>
    }

    interface Interface {
        val methods: Map<String, List<NamedFunctionSignature>>
    }

    interface Struct {
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
        val embedded: Set<Lazy<StructDefinition>>

        interface Field : Element, Accessible<Field> {
            val mutable: Boolean
            val type: Type
            override val name: Located<String>
        }
    }
}