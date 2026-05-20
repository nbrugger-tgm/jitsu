package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.CompilerMessage
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.behaviour.Finalizable
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeAware
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.Collections.emptyList

typealias ResolveGenericFn = (TypeDefinition.DirectTypeDefinition.TypeParameter, messages: CompilerMessages) -> Type;

@Serializable
sealed interface Type : Element {
    /**
     * Reduces the type to its "raw" form by resolving all [references][TypeReference] from the graph and substituting GenericTypes/Type parameters when required
     *
     * _Implementation Note_: If no generics are being substituted the result can be cached
     *
     * @param resolveGeneric substitution function for [generics][TypeDefinition.DirectTypeDefinition.TypeParameter]. If null no substitution will happen, if set the type returned by the function will be used
     */
    fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn? = null): Type

    fun acceptsInstanceOf(type: Type): ReasonedBoolean {
        return if (type is Undefined)
            ReasonedBoolean.True("While UNDEFINED cannot be assigned to anything, the error lies in the definition of the type not its usage");
        else if (type is Union) {
            val optionAssignability = type.options.map { mapAssignabilityBoolean(acceptsInstanceOf(it), it, this) }
            if (optionAssignability.all { boolean -> boolean.value }) ReasonedBoolean.True(
                "Each type in the union is assignable to $this",
                *optionAssignability.toTypedArray()
            )
            else {
                val assignWholeUnion = accepts(type)
                if (assignWholeUnion.value) return assignWholeUnion;
                ReasonedBoolean.False(
                    "Not all types in the union ($type), nor the union itself are/is assignable to $this",
                    *(optionAssignability.filter { !it.value } + assignWholeUnion).toTypedArray()
                )
            }
        } else if (type is TypeReference) {
            acceptsInstanceOf(type.typeCache)
        } else {
            val reason = accepts(type)
            if (!reason.value) ReasonedBoolean.False("$type not assignable to $this", reason)
            else reason
        }
    }

    private fun mapAssignabilityBoolean(boolean: ReasonedBoolean, from: Type, to: Type): ReasonedBoolean {
        return if (boolean.value) ReasonedBoolean.True("$from is assignable to $to", boolean)
        else ReasonedBoolean.False("$from is not assignable to $to", boolean)
    }

    fun accepts(type: Type): ReasonedBoolean

    sealed interface Primitive : Type {
        val size: BitSize
    }

    @Serializable
    data class Int(override val size: BitSize) : Primitive {
        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type = this
        override fun toString(): String {
            return "i${size.bits}"
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is Int && type.size.bits <= this.size.bits) ReasonedBoolean.True(
                "integers accept integers their size and smaller"
            )
            else if (type is UInt && type.size.bits * 2 <= this.size.bits) ReasonedBoolean.True(
                "integers accept unsigned integers that are at most half their size"
            )
            else if (type is Float) ReasonedBoolean.False("Integers only accept integers. To assign non int numbers convert them first")
            else if (type is Int) ReasonedBoolean.False("Integers only accept integers their size or less, since assigning for example a i64 to a i32 can cause number overflow")
            else if (type is UInt) ReasonedBoolean.False("Integers only accept unsigned integers that are at most half their size (you can assign u32 to i64 but not u32 to i32)")
            else ReasonedBoolean.False("$type cannot be assigned to $this")
        }

        @Transient
        override val children: List<Element> = emptyList()
    }

    @Serializable
    data class UInt(override val size: BitSize) : Primitive {
        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type = this
        override fun toString(): String {
            return "u${size.bits}"
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is UInt && type.size.bits <= this.size.bits) ReasonedBoolean.True(
                "unsigned integers accept unsigned integers their size and smaller"
            )
            else if (type is UInt) ReasonedBoolean.False("$type is too large to fit into a $this")
            else if (type is Int || type is Float) ReasonedBoolean.False("Unsigned integers only accept unsigned integers. To assign non uint numbers convert them first")
            else ReasonedBoolean.False("$type cannot be assigned to $this")
        }

        @Transient
        override val children: List<Element> = emptyList()

    }

    @Serializable
    data class Float(override val size: BitSize = BitSize.BIT_32) : Primitive {
        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type = this
        override fun toString(): String {
            return "f${size.bits}"
        }

        override fun accepts(type: Type): ReasonedBoolean {
            if (type is Float && type.size.bits <= this.size.bits) return ReasonedBoolean.True(
                "floats accept floats their size and smaller"
            )
            if (type is Float) return ReasonedBoolean.False("Floats only accept floats their size or smaller")
            return ReasonedBoolean.False("$type cannot be assigned to $this")
        }

        @Transient
        override val children: List<Element> = emptyList()

    }

    @Serializable
    data class Value(val value: Constant<@Contextual Any>) : Type {
        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type = this
        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is Value && value == type.value) ReasonedBoolean.True("$value is the same as ${type.value}")
            else if (type is Value) ReasonedBoolean.True("$value is not the same as ${type.value}")
            else ReasonedBoolean.True("$type is not a value type, but $this is")
        }

        @Transient
        override val children: List<Element> = listOf(value)
        override fun toString(): String {
            return value.value.toString()
        }
    }

    @Serializable
    data object Null : Type {
        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type = this
        override fun toString(): String {
            return "null"
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is Null) ReasonedBoolean.True("null is assignable to the null type")
            else ReasonedBoolean.False("null is the only value assignable to the null type")
        }

        @Transient
        override val children: List<Element> = emptyList()

    }

    /**
     * This type is not usable in the language. It is the type used at compile time when a type is not resolvable
     */
    @Serializable
    data object Undefined : Type {
        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type = this
        override fun accepts(type: Type): ReasonedBoolean {
            return ReasonedBoolean.True("While the UNDEFINED type does not accept any types, the error is to be treated at the source (the type definition) an not its usage")
        }

        @Transient
        override val children: List<Element> = emptyList()
    }

    @Serializable
    data class Array(
        val elementType: Type,
        val size: kotlin.Int?,
        val dimensions: kotlin.Int = 1
    ) : Type {

        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type {
            return Array(elementType.rawType(messages, resolveGeneric), size, dimensions)
        }

        override fun accepts(type: Type): ReasonedBoolean {
            if (type !is Array) return ReasonedBoolean.False("$type is not an array and can therefore not be assigned to an array")
            val elementsAccept = this.elementType.acceptsInstanceOf(type.elementType)
            return if (elementsAccept.value) {
                if (this.size != null && type.size != this.size) {
                    ReasonedBoolean.False("$this and $type have differing fixed sizes")
                } else ReasonedBoolean.True("$type is an array with an assignable element type", elementsAccept)
            } else {
                ReasonedBoolean.False(
                    "Element type of $type (${type.elementType}) is not compatible with ${this.elementType}",
                    elementsAccept
                )
            }
        }

        @Transient
        override val children: List<Element> = listOfNotNull(elementType)

        override fun toString(): String {
            return "$elementType[${size?.toString() ?: ""}]"
        }
    }

    @Serializable
    data object Boolean : Primitive {
        override fun toString(): String {
            return "boolean"
        }

        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type = this

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is Boolean) {
                ReasonedBoolean.True("boolean is assignable to boolean")
            } else {
                ReasonedBoolean.False("only boolean is assignable to boolean")
            }
        }

        @Transient
        override val children: List<Element> = emptyList()
        override val size: BitSize
            get() = BitSize.BIT_1
    }

    @Serializable
    data class FunctionTypeSignature(val returnType: Type?, val parameters: List<Parameter>) : Type {
        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type {
            return FunctionTypeSignature(
                returnType?.rawType(messages, resolveGeneric),
                parameters.map { it.copy(type = it.type.rawType(messages, resolveGeneric)) })
        }

        override fun accepts(type: Type): ReasonedBoolean {
            TODO("Not yet implemented")
        }

        @Serializable
        data class Parameter(val name: Located<String>, val type: Type, var optional: kotlin.Boolean) : Element {
            @Transient
            override val children: List<Element> = listOf(type)
            override fun toString(): String {
                return "$name: $type"
            }
        }

        @Transient
        override val children: List<Element> = parameters + listOfNotNull(returnType)

        override fun toString(): String {
            return "(${parameters.joinToString(", ") { "${it.type}" }}) -> ${returnType ?: "void"}"
        }
    }

    @Serializable
    data class TypeReference(
        override val reference: Located<String>,
        val genericParameters: List<Located<Type>>
    ) : AccessImpl<TypeDefinition>(), Type, Access.TypeAccess, ScopeAware, Finalizable {
        @Transient
        override val restore = JitsuModule::getType
        @Transient
        override val getSymbolId: JitsuModule.(TypeDefinition) -> SymbolID = JitsuModule::getSymbolID

        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type {
            return when (val target = target) {
                null -> Undefined
                is TypeDefinition.DirectTypeDefinition -> target.rawType(messages, resolveGeneric)
                is TypeDefinition.ParameterizedType -> resolveParameterized(messages, target, resolveGeneric)
            }
        }

        private fun resolveParameterized(
            messages: CompilerMessages,
            target: TypeDefinition.ParameterizedType,
            resolveGeneric: ResolveGenericFn?
        ): Type {
            val resolvedGenerics = genericParameters.mapIndexedNotNull { index, type ->
                val resolved = type.value.rawType(messages, resolveGeneric)
                val targetGenericName = target.generics.getOrNull(index)?.name?.value
                if (targetGenericName == null) {
                    messages.error(
                        "Generic parameter $resolved ($index) does not exist in the definition of $target",
                        type.location,
                        if (!target.generics.isEmpty())
                            CompilerMessage.Hint(
                                "Generics of target type are defined here",
                                target.generics.first().name.location.rangeTo(target.generics.last().name.location)
                            )
                        else
                            CompilerMessage.Hint(
                                "Target type has no generics defined",
                                target.name.location
                            )
                    )
                    null
                } else resolved to targetGenericName
            }.associateBy({ it.second }, { it.first })
            return target.toType(messages, resolvedGenerics)
        }

        @Transient lateinit var typeCache: Type
        override fun accepts(type: Type): ReasonedBoolean {
            return this.typeCache.acceptsInstanceOf(type)
        }

        @Transient
        override val children: List<Element> = genericParameters.map { it.value }

        override fun resolve(messages: CompilerMessages): TypeDefinition? {
            target?.let { return it }
            val resolveType = scope.resolveType(reference, messages)
            if (resolveType != null) {
                setResolvedTarget(resolveType)
            }
            return resolveType
        }

        override fun toString(): String {
            return reference.value + if (!genericParameters.isEmpty()) genericParameters.joinToString(
                prefix = "<",
                postfix = ">",
                separator = ", "
            ) { it.value.toString() } else ""
        }

        override fun finalize(messages: CompilerMessages) {
            typeCache = rawType(messages)
        }
    }

    @Serializable
    class Union(var options: List<Type>) : Type {
        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type {
            val resolvedOptions = options.map { it.rawType(messages, resolveGeneric) }
                .flatMap { type -> if (type is Union) type.options else listOf(type) }
                .distinct()
            return if (resolvedOptions.size > 1) Union(resolvedOptions)
            else resolvedOptions.single()
        }

        override fun accepts(type: Type): ReasonedBoolean {
            val optionsAccept = options.map { it.toString() to it.acceptsInstanceOf(type) }
            val matches = optionsAccept.filter { it.second.value }
            if (matches.isNotEmpty()) return ReasonedBoolean.True(
                "$type is assignable to one or more options of $this",
                causes = matches
            )
            return ReasonedBoolean.False(
                "$type is not assignable to any of: ${options.joinToString(", ")}",
                causes = optionsAccept
            )
        }

        override val children: List<Element> get() = options
        override fun toString(): String {
            return options.joinToString(" | ")
        }
    }

    @Serializable
    data class StructuralInterface(val fields: Map<String, TypeDefinition.ParameterizedType.Struct.Field>) : Type {
        override fun rawType(messages: CompilerMessages, resolveGeneric: ResolveGenericFn?): Type {
            return StructuralInterface(fields.mapValues { (_, b) ->
                b.copy(
                    type = b.type.rawType(
                        messages,
                        resolveGeneric
                    )
                )
            })
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return ReasonedBoolean.False("structural interface assignability not implemented yet")
        }

        override val children: List<Element> get() = fields.values.toList()
        override fun toString(): String {
            return "{${fields.entries.joinToString(", ")}}"
        }
    }
}
