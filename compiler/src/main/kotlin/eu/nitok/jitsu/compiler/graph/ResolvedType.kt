package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class ResolvedType {
    object String : ResolvedType()

    @Serializable
    data class Int(val bit32: BitSize) : ResolvedType()

    @Serializable
    data class UInt(val bit32: BitSize) : ResolvedType()

    @Serializable
    data class Value(val value: Constant<@Contextual Any>) : ResolvedType() {
        val valueType: ResolvedType = value.type
    }

    @Serializable
    sealed class ComplexType() : ResolvedType() {
        abstract val mutable: kotlin.Boolean;

        @Serializable
        data class Array(
            val type: ResolvedType,
            val size: Int?,
            val dimensions: kotlin.Int = 1,
            override val mutable: kotlin.Boolean
        ) : ComplexType() {

        }


        @Serializable
        data class Struct(
            val struct: StructDefinition,
            override val mutable: kotlin.Boolean
        )  : ComplexType() {}
    }

    sealed class Reference() : ResolvedType() {
        @Serializable
        data class Struct(val struct: ComplexType.Struct) : Reference()

        @Serializable
        data class Array(val array: ComplexType.Array) : Reference()

        @Serializable
        data class Function(val returnType: ResolvedType, val parameters: List<Parameter>) : Reference() {
            @Serializable
            data class Parameter(val type: ResolvedType, val mutable: kotlin.Boolean)
        }
    }

    class Boolean : ResolvedType() {}
}