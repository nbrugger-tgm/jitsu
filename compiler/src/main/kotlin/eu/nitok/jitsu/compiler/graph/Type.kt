package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.diagnostic.CompilerMessage
import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration.Companion.milliseconds

@Serializable
sealed interface Type : Element {
    fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type

    fun acceptsInstanceOf(type: Type): ReasonedBoolean {
        return if (type is Undefined) ReasonedBoolean.True("While UNDEFINED cannot be assigned to anything, the error lies in the definition of the type not its usage");
        else if (type is Union) {
            var optionAssignability = type.options.map { mapAssignabilityBoolean(accepts(it), it, this) }
            if (optionAssignability.all { boolean -> boolean.value })
                ReasonedBoolean.True(
                    "Each type in the union is assignable to $this",
                    *optionAssignability.toTypedArray()
                )
            else
                ReasonedBoolean.False(
                    "Not all types in the union ($type) are assignable to $this",
                    *optionAssignability.filter { !it.value }.toTypedArray()
                )
        } else mapAssignabilityBoolean(accepts(type), type, this)
    }

    fun mapAssignabilityBoolean(boolean: ReasonedBoolean, from: Type, to: Type): ReasonedBoolean {
        return if (boolean.value) ReasonedBoolean.True("$from is assignable to $to", boolean)
        else ReasonedBoolean.False("$from is not assignable to $to", boolean)
    }

    fun accepts(type: Type): ReasonedBoolean

    @Serializable
    data class Int(val size: BitSize) : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
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
    data class UInt(val size: BitSize) : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
        override fun toString(): String {
            return "u${size.bits}"
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is UInt && type.size.bits <= this.size.bits) ReasonedBoolean.True(
                "unsigned integers accept unsigned integers their size and smaller"
            )
            else if (type is Int || type is Float) ReasonedBoolean.False("Unsigned integers only accept unsigned integers. To assign non uint numbers convert them first")
            else ReasonedBoolean.False("$type cannot be assigned to $this")
        }

        @Transient
        override val children: List<Element> = emptyList()

    }

    @Serializable
    data class Float(val size: BitSize = BitSize.BIT_32) : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
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
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
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
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
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
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this
        override fun accepts(type: Type): ReasonedBoolean {
            return ReasonedBoolean.True("While the UNDEFINED type does not accept any types, the error is to be treated at the source (the type definition) an not its usage")
        }

        @Transient
        override val children: List<Element> = emptyList()
    }

    @Serializable
    data class Array(
        val type: Type,
        val size: Expression?,
        val dimensions: kotlin.Int = 1
    ) : Type {

        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            return Array(type.resolve(messages, generics), size, dimensions)
        }

        override fun accepts(type: Type): ReasonedBoolean {
            if (type !is Array) return ReasonedBoolean.False("$type is not an array and can therefore not be assigned to an array")
            val elementsAccept = this.type.acceptsInstanceOf(type.type)
            return if (elementsAccept.value) {
                ReasonedBoolean.True("$type is an array with an assignable element type", elementsAccept)
            } else {
                ReasonedBoolean.False(
                    "Element type of $type (${type.type}) is not compatible with ${this.type}",
                    elementsAccept
                )
            }
        }

        @Transient
        override val children: List<Element> = listOfNotNull(type, size)

        override fun toString(): String {
            return "$type[${size?.toString() ?: ""}]"
        }
    }

    @Serializable
    data object Boolean : Type {
        override fun toString(): String {
            return "boolean"
        }

        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type = this

        override fun accepts(type: Type): ReasonedBoolean {
            return if (type is Boolean) {
                ReasonedBoolean.True("boolean is assignable to boolean")
            } else {
                ReasonedBoolean.False("only boolean is assignable to boolean")
            }
        }

        @Transient
        override val children: List<Element> = emptyList()
    }

    @Serializable
    data class FunctionTypeSignature(val returnType: Type?, val parameters: List<Parameter>) : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            return FunctionTypeSignature(
                returnType?.resolve(messages, generics),
                parameters.map { it.copy(type = it.type.resolve(messages, generics)) })
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
    ) : Type, Access.TypeAccess, ScopeAware {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            var target = target
            if (target == null) {
                val resolved = resolveAccessTarget(messages)
                target = resolved
                this.target = resolved
            }
            return when (target) {
                is TypeDefinition.DirectTypeDefinition -> target.resolve(messages, generics)
                is TypeDefinition.TypeParameter -> generics[reference.value]
                    ?: this//maybe a dedicated "GenericType" should be created
                is TypeDefinition.ParameterizedType -> {
                    var resolvedGenerics =
                        genericParameters.mapIndexedNotNull<Located<Type>, Pair<Type, String>> { index, type ->
                            val resolved = type.value.resolve(messages, generics)
                            var targetGenericName = target.generics.getOrNull(index)?.name?.value
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
                        }.associateBy({it.second},{it.first})
                    target.toType(messages, resolvedGenerics)
                }
            }
        }

        public lateinit var resolvedCache: Type
        override fun accepts(type: Type): ReasonedBoolean {
            if (!this::resolvedCache.isInitialized) throw Error("Type reference $this cannot be used in type checking. Resolve it first")
            if (resolvedCache == this) {
                if (type is TypeReference) {
                    if (type.reference.value != this.reference.value) {
                        return ReasonedBoolean.False("${type.reference.value} is not guaranteed to match ${this.reference.value}")
                    }
                    if (type.genericParameters.size != this.genericParameters.size) {
                        return ReasonedBoolean.False("Type references have different number of generic parameters")
                    }
                    for ((i, value) in this.genericParameters.mapIndexed { i, b -> i to b }) {
                        if (type.genericParameters.size <= i) {
                            return ReasonedBoolean.False("$type does not have generic parameter $i")
                        }
                        val accepts = type.genericParameters[i].value.acceptsInstanceOf(value.value)
                        if (!accepts.value) {
                            return ReasonedBoolean.False("$i of $type is not assignable to $value", accepts)
                        }
                    }
                    return ReasonedBoolean.True("$type references the same type as $this")
                } else {
                    return ReasonedBoolean.False("$type is not assignable to $this")
                }
            }
            return resolvedCache.accepts(type)
        }

        @Transient
        override val children: List<Element> = genericParameters.map { it.value }

        @Transient
        override var target: TypeDefinition? = null;

        @Transient
        override lateinit var accessor: Accessor;

        @Transient
        lateinit var scope: Scope;

        override fun resolveAccessTarget(messages: CompilerMessages): TypeDefinition {
            target?.let { return it }
            return scope.resolveType(reference, messages)
        }

        override fun setEnclosingScope(parent: Scope) {
            scope = parent
        }

        override fun toString(): String {
            return reference.value + if (!genericParameters.isEmpty()) genericParameters.joinToString(
                prefix = "<",
                postfix = ">",
                separator = ", "
            ) { it.value.toString() } else ""
        }

        override fun finalize(messages: CompilerMessages) {
            super.finalize(messages)
            resolvedCache = resolve(messages, mapOf())
        }
    }

    @Serializable
    class Union(var options: List<Type>) : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            return Union(options.map { it.resolve(messages, generics) }
                .flatMap { type -> if (type is Union) type.options else listOf(type) }
                .distinct()
            )
        }

        override fun accepts(type: Type): ReasonedBoolean {
            var optionsAccept = options.map { it.acceptsInstanceOf(type) }
            var matches = optionsAccept.filter { it.value }
            if (matches.isNotEmpty()) return ReasonedBoolean.True(
                "$type is assignable to one or more options of $this",
                *matches.toTypedArray()
            )
            return ReasonedBoolean.False(
                "None of the types in $this can be assigned an instance of $type",
                *optionsAccept.toTypedArray()
            )
        }

        override val children: List<Element> get() = options
        override fun toString(): String {
            return options.joinToString(" | ")
        }
    }

    @Serializable
    data class StructuralInterface(val fields: Map<String, TypeDefinition.ParameterizedType.Struct.Field>) : Type {
        override fun resolve(messages: CompilerMessages, generics: Map<String, Type>): Type {
            return StructuralInterface(fields.mapValues { (_, b) -> b.copy(type = b.type.resolve(messages, generics)) })
        }

        override fun accepts(type: Type): ReasonedBoolean {
            return ReasonedBoolean.False("structural inferface assignability not implemented yet")
        }

        override val children: List<Element> get() = fields.values.toList()
        override fun toString(): String {
            return "{${fields.entries.joinToString(", ")}}"
        }
    }
}
