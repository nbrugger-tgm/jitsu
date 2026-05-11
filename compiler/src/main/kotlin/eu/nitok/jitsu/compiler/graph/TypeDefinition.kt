package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.ReasonedBoolean

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A template for a [Type] not a type by itself
 */
@Serializable
sealed class TypeDefinition : Accessible<TypeDefinition>, Element {
    abstract override val name: Located<String>
    @Transient override val accessToSelf: MutableList<Access<TypeDefinition>> = mutableListOf()
    @Transient override lateinit var module: JitsuModule
        internal set;

    override fun setEnclosingModule(parent: JitsuModule) {
        this.module = parent
    }

    /**
     * A type that is not directly usable since it is parameterized
     *
     * Examples:
     * - List&lt;T>
     * - Either&lt;A,B>
     * - Optional&lt;T>
     *
     * These types are only full types when referenced with their parameters filled like `List<String>`
     *
     * To form a [Type] from a [ParameterizedType] you need a [Type.TypeReference]
     */
    sealed class ParameterizedType : TypeDefinition() {
        abstract val generics: List<TypeParameter>
        abstract fun toType(messages: CompilerMessages, typeParameters: Map<String, Type>): Type;
        @Serializable
        data class Struct(
            override val name: Located<String>,
            override val generics: List<TypeParameter>,
            private val fields: MutableSet<Field>,
            private val embedded: MutableSet<Lazy<Struct>> = mutableSetOf()
        ) : ParameterizedType() {
            @Serializable
            data class Field(override val name: Located<String>, var mutable: kotlin.Boolean, val type: Type) : Element,
                Accessible<Field> {
                override val children: List<Element> get() = listOf(type)
                @Transient override val accessToSelf: MutableList<Access<Field>> = mutableListOf()
                @Transient override lateinit var module: JitsuModule
                    internal set;
                override fun setEnclosingModule(parent: JitsuModule) {
                    this.module = parent
                }
            }

            val allFields: Set<Field> get() = embedded.flatMap { it.value.allFields }.toSet() + fields
            override val children: List<Element> get() = fields.toList() + embedded.map { it.value }
            override fun toType(
                messages: CompilerMessages,
                typeParameters: Map<String, Type>
            ): Type {
                TODO("Not yet implemented")
            }
        }

        @Serializable
        data class Alias(
            override val name: Located<String>,
            override val generics: List<TypeParameter>,
            var type: Type
        ) : ParameterizedType(), ScopeProvider, ScopeAware {
            override val children: List<Element> get() = listOf(type) + generics
            override fun toType(messages: CompilerMessages, typeParameters: Map<String, Type>): Type {
                return type.resolveType(messages, typeParameters);
            }

            @Transient override val scope: Scope = Scope(types = generics.associateBy { it.name.value })

            override fun setEnclosingScope(parent: Scope) {
                scope.parent = parent
            }

            override fun toString(): String {
                return "${name.value}${if (generics.isNotEmpty()) "<${generics.joinToString(", ")}>" else ""}"
            }
        }

        @Serializable
        data class Interface(
            override val name: Located<String>,
            override val generics: List<TypeParameter>,
            val methods: Map<String, List<NamedFunctionSignature>>
        ) : ParameterizedType() {
            constructor(
                name: Located<String>,
                generics: List<TypeParameter>,
                methods: List<NamedFunctionSignature>
            ) : this(name, generics, methods.groupBy { it.name.value })

            override val children: List<Element> get() = methods.values.flatten()
            override fun toType(
                messages: CompilerMessages,
                typeParameters: Map<String, Type>
            ): Type {
                TODO("Not yet implemented")
            }
        }

        @Serializable
        data class Class(
            override val name: Located<String>,
            override val generics: List<TypeParameter>,
            val fields: List<Struct.Field>,
            val methods: List<Function>
        ) : ParameterizedType() {
            override val children: List<Element>
                get() = fields + methods // + generics

            override fun toType(
                messages: CompilerMessages,
                typeParameters: Map<String, Type>
            ): Type {
                TODO("Not yet implemented")
            }

        }
    }

    /**
     * A [Type] template that is specific enought to be a type by itself
     */
    sealed class DirectTypeDefinition : TypeDefinition(), Type {
        @Serializable
        data class Enum(
            override val name: Located<String>,
            val constants: List<Constant>
        ) : DirectTypeDefinition() {
            override val children: List<Element> get() = constants
            override fun resolveType(
                messages: CompilerMessages,
                generics: Map<String, Type>
            ): Type {
                return this
            }

            override fun accepts(type: Type): ReasonedBoolean {
                return if (type == this) ReasonedBoolean.True("$type is the same enum as $this")
                else if (type is Enum) ReasonedBoolean.False("$type is not the same enum as $this")
                else ReasonedBoolean.False("$type is not an enum")
            }

            init {
                constants.forEach { it.enum = this }
            }

            @Serializable
            data class Constant(override val name: Located<String>) : Element, Accessible<Constant> {
                override val children: List<Element> get() = listOf()
                @Transient override val accessToSelf: MutableList<Access<Constant>> = mutableListOf()
                @Transient lateinit var enum: Enum
                @Transient override lateinit var module: JitsuModule
                    internal set;

                override fun setEnclosingModule(parent: JitsuModule) {
                    this.module = parent
                }
            }
        }
    }

    @Serializable
    class TypeParameter(
        override val name: Located<String>
    ) : TypeDefinition() {
        @Transient
        override val children: List<Element> = emptyList();

        override fun toString(): String {
            return name.value
        }
    }
}