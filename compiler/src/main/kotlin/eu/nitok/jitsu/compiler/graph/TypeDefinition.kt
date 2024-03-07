package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable

@Serializable
sealed class TypeDefinition : Accessible<TypeDefinition> {
    abstract override val name: Located<String>
    override val accessToSelf: MutableList<in Access<TypeDefinition>> = mutableListOf()
    @Serializable
    data class Struct(
        override val name: Located<String>,
        private val fields: MutableSet<Field>,
        val generics: List<Located<String>>,
        private val embedded: MutableSet<Lazy<Struct>> = mutableSetOf()
    ) : TypeDefinition() {
        @Serializable
        data class Field(val name: kotlin.String, var mutable: kotlin.Boolean, val type: Lazy<Type>)

        val allFields: Set<Field> get() = embedded.flatMap { it.value.allFields }.toSet() + fields
    }

    @Serializable
    data class Enum(
        override val name: Located<String>,
        val constants: List<Located<String>>
    ) : TypeDefinition()

    @Serializable
    data class Alias(
        override val name: Located<String>,
        val generics: List<Located<String>>,
        var type: Lazy<Type>
    ) : TypeDefinition()

    @Serializable
    data class Interface(
        override val name: Located<String>,
        val generics: List<Located<String>>,
        val methods: Map<kotlin.String, Located<Type.FunctionTypeSignature>>
    ) : TypeDefinition()
}