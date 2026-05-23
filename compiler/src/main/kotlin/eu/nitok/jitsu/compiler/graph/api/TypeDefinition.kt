package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located

/**
 * A template for a [eu.nitok.jitsu.compiler.graph.api.Type] not a type by itself
 */
sealed interface TypeDefinition : Accessible<TypeDefinition>, Element {
    override val name: Located<String>
    /**
     * A type that may not directly usable since it may parameterized
     *
     * Examples:
     * - List&lt;T>
     * - Either&lt;A,B>
     * - Optional&lt;T>
     *
     * These types are only full types when referenced with their parameters filled like `List<String>`
     *
     * To form a [eu.nitok.jitsu.compiler.graph.api.Type] from a [ParameterizedType] you need a [eu.nitok.jitsu.compiler.graph.api.Type.TypeReference]
     */
    sealed interface ParameterizedType: TypeDefinition {
        val generics: List<DirectTypeDefinition.TypeParameter>

        interface Struct : ParameterizedType {
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
            val embedded: Set<Lazy<Struct>>

            interface Field : Element, Accessible<Field> {
                val mutable: Boolean
                val type: Type
                override val name: Located<String>
            }
        }

        interface Alias : ParameterizedType {
            val type: Type
        }

        interface Interface : ParameterizedType {
            val methods: Map<String, List<NamedFunctionSignature>>
        }

        interface Class : ParameterizedType {
            val fields: List<Struct.Field>
            val methods: List<Function>
        }
    }
    /**
     * A [eu.nitok.jitsu.compiler.graph.api.Type] template that is specific enough to be a type by itself
     */
    sealed interface DirectTypeDefinition : TypeDefinition, Type {
        interface TypeParameter : DirectTypeDefinition
        interface Enum : DirectTypeDefinition {
            val constants: List<Constant>
            interface Constant : Element, Accessible<Constant> {
                val enum: Enum
                override val name: Located<String>
            }
        }
    }
}