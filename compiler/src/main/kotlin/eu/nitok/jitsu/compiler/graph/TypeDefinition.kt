package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

@Serializable
sealed class TypeDefinition : Accessible<TypeDefinition>, Element {
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
        data class Field(override val name: Located<String>, var mutable: kotlin.Boolean, val type: Type): Element, Accessible<Field> {
            override val children: List<Element> get() = listOf(type)
            @Transient override val accessToSelf: MutableList<in Access<Field>> = mutableListOf()
        }

        val allFields: Set<Field> get() = embedded.flatMap { it.value.allFields }.toSet() + fields
        override val children: List<Element> get() = fields.toList() + embedded.map { it.value }
    }

    @Serializable
    data class Enum(
        override val name: Located<String>,
        val constants: List<Constant>
    ) : TypeDefinition(){
        override val children: List<Element> get() = constants
        init {
            constants.forEach { it.enum = this }
        }
        @Serializable
        data class Constant(override val name: Located<String>): Element, Accessible<Constant> {
            override val children: List<Element> get() = listOf()
            @Transient override val accessToSelf: MutableList<in Access<Constant>> = mutableListOf()
            @Transient lateinit var enum: Enum
        }
    }

    @Serializable
    data class Alias(
        override val name: Located<String>,
        val generics: List<Located<String>>,
        var type: Type
    ) : TypeDefinition() {
        override val children: List<Element> get() = listOf(type)
    }

    @Serializable
    data class Interface(
        override val name: Located<String>,
        val generics: List<Located<String>>,
        val methods: Map<String, List<NamedFunctionSignature>>
    ) : TypeDefinition() {
        constructor(name: Located<String>, generics: List<Located<String>>, methods: List<NamedFunctionSignature>) : this(name, generics, methods.groupBy { it.name.value })
        override val children: List<Element> get() = methods.values.flatten()
    }

    @Serializable
    data class Class(
        override val name: Located<String>,
        val generics: List<Located<String>>,
        val fields: List<Struct.Field>,
        val methods: List<Function>
    ): TypeDefinition() {
        override val children: List<Element>
            get() = fields + methods // + generics

    }
}