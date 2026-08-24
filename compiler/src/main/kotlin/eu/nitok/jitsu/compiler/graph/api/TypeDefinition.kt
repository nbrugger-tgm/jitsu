package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located

/**
 * A template for a [eu.nitok.jitsu.compiler.graph.api.Type] not a type by itself
 */
sealed interface TypeDefinition<T: TypeDefinition<T>> : Accessible<T>, Element {
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
    sealed interface ParameterizedType: TypeDefinition<ParameterizedType>, Parameterized {

        interface Struct : StructStructure, ParameterizedType

        interface Interface : InterfaceStructure, ParameterizedType

        interface Class : ClassStructure, ParameterizedType

        interface Alias : ParameterizedType {
            val type: Type
        }
    }
    /**
     * A [eu.nitok.jitsu.compiler.graph.api.Type] template that is specific enough to be a type by itself
     */
    sealed interface DirectTypeDefinition : TypeDefinition<DirectTypeDefinition>, Type {
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