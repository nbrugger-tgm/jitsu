package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Type : Element {
    @Transient
    override val children: List<Element> = listOf()

    @Serializable
    data class Int(val bits: BitSize) : Type()

    @Serializable
    data class UInt(val bits: BitSize) : Type()

    @Serializable
    data class Float(val bits: BitSize = BitSize.BIT_32) : Type()

    @Serializable
    data class Value(val value: Constant<@Contextual Any>) : Type() {
        val valueType: Type = value.type
        @Transient
        override val children: List<Element> = listOf(value)
    }

    @Serializable
    data object Null : Type()

    /**
     * This type is not usable in the language. It is the type used at compile time when a type is not resolvable
     */
    @Serializable
    data object Undefined : Type()

    @Serializable
    data class Array(
        val type: Type,
        val size: Expression?,
        val dimensions: kotlin.Int = 1
    ) : Type() {
        @Transient
        override val children: List<Element> = listOfNotNull(type, size)
    }

    @Serializable
    data object Boolean : Type()

    @Serializable
    data class FunctionTypeSignature(val returnType: Type?, val params: List<Parameter>) : Type() {
        @Serializable
        data class Parameter(val name: Located<String>, val type: Type) : Element {
            @Transient
            override val children: List<Element> = listOf(type)
        }

        @Transient
        override val children: List<Element> = params + listOfNotNull(returnType)
    }

    @Serializable
    data class TypeReference(
        override val reference: Located<String>,
        val genericParameters: Map<String, Type>
    ) : Type(), Access.TypeAccess {
        @Transient
        override val children: List<Element> = genericParameters.values.toList()
        override val target: TypeDefinition
            get() = accessor.scope.resolveType(reference)
        @Transient
        private lateinit var _accessor: Accessor
        override var accessor: Accessor
            get() = _accessor
            set(value) { _accessor = value }
    }
}