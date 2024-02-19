package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.ast.AstNode
import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed class ResolvedType {
    data object String : ResolvedType()

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

        @Serializable
        data class Array(
            val type: AstNode<ResolvedType>,
            val size: Expression?,
            val dimensions: kotlin.Int = 1
        ) : ComplexType() {
        }

        @Serializable
        class Struct(
            private val fields: MutableSet<Field>,
            val embedded: MutableSet<Struct> = mutableSetOf()
        ) : ComplexType() {
            @Serializable
            data class Field(val name: kotlin.String, var mutable: kotlin.Boolean, val type: ResolvedType)

            val allFields: Set<Field> get() = embedded.flatMap { it.allFields }.toSet() + fields
        }
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

    @Serializable
    data object Boolean : ResolvedType()
    @Serializable
    data class Enum(val constants: List<Located<kotlin.String>>) : ResolvedType() {
    }

    @Serializable
    class Float(val bitSize: BitSize = BitSize.BIT_32) : ResolvedType() {
    }

    @Serializable
    data class FunctionTypeSignature(val returnType: ResolvedType?, val params: List<Parameter>) : ResolvedType(){
        @Serializable
        data class Parameter(val name: Located<kotlin.String>, val type: ResolvedType)
    }

    @Serializable
    data class Interface(val methods: Map<kotlin.String, Located<FunctionTypeSignature>>) : ResolvedType() {

    }

    data class Named(val name: Located<kotlin.String>) {
        var type: ResolvedType? = null
    }
}