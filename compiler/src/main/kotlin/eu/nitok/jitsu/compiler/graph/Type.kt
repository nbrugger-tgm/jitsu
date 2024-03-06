package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class Type {
    @Serializable
    data class Int(val bits: BitSize) : Type()

    @Serializable
    data class UInt(val bits: BitSize) : Type()

    @Serializable
    data class Float(val bits: BitSize = BitSize.BIT_32) : Type()

    @Serializable
    data class Value(val value: Constant<@Contextual Any>) : Type() {
        val valueType: Type = value.type
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
    ) : Type()

    @Serializable
    data object Boolean : Type()

    @Serializable
    data class FunctionTypeSignature(val returnType: Type?, val params: List<Parameter>) : Type() {
        @Serializable
        data class Parameter(val name: Located<String>, val type: Type)
    }

    @Serializable
    data class TypeReference(val typedef: Lazy<TypeDefinition>, val genericParameters: Map<String, Type>):Type()
}