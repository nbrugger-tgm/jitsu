package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
sealed interface ResolvedType {
    data object String : ResolvedType

    @Serializable
    data class Int(val bits: BitSize) : ResolvedType

    @Serializable
    data class UInt(val bits: BitSize) : ResolvedType

    @Serializable
    data class Float(val bits: BitSize = BitSize.BIT_32) : ResolvedType

    @Serializable
    data class Value(val value: Constant<@Contextual Any>) : ResolvedType {
        val valueType: ResolvedType = value.type
    }

    data object Null : ResolvedType

    /**
     * This type is not usable in the language. It is the type used at compile time when a type is not resolvable
     */
    data object Undefined : ResolvedType

    @Serializable
    data class Array(
        val type: Lazy<ResolvedType>,
        val size: Expression?,
        val dimensions: kotlin.Int = 1
    ) : ResolvedType

    @Serializable
    data object Boolean : ResolvedType

    @Serializable
    data class FunctionTypeSignature(val returnType: Lazy<ResolvedType>?, val params: List<Parameter>) : ResolvedType {
        @Serializable
        data class Parameter(val name: Located<kotlin.String>, val type: Lazy<ResolvedType>)
    }

    @Serializable
    sealed interface NamedType : ResolvedType {
        val name: Located<kotlin.String>

        @Serializable
        class NamedStruct(
            override val name: Located<kotlin.String>,
            private val fields: MutableSet<Field>,
            private val embedded: MutableSet<Lazy<NamedStruct>> = mutableSetOf()
        ) : NamedType {
            @Serializable
            data class Field(val name: kotlin.String, var mutable: kotlin.Boolean, val type: Lazy<ResolvedType>)

            val allFields: Set<Field> get() = embedded.flatMap { it.value.allFields }.toSet() + fields
        }

        @Serializable
        data class Enum(
            override val name: Located<kotlin.String>,
            val constants: List<Located<kotlin.String>>
        ) : NamedType

        @Serializable
        data class Alias(override val name: Located<kotlin.String>, var type: Lazy<ResolvedType>) : NamedType

        @Serializable
        data class Interface(
            override val name: Located<kotlin.String>,
            val methods: Map<kotlin.String, Located<FunctionTypeSignature>>
        ) : NamedType
    }

//    sealed class Reference : ResolvedType() {
//        @Serializable
//        data class Struct(val struct: ComplexType.Struct) : Reference()
//
//        @Serializable
//        data class Array(val array: ComplexType.Array) : Reference()
//
//        @Serializable
//        data class Function(val returnType: ResolvedType, val parameters: List<Parameter>) : Reference() {
//            @Serializable
//            data class Parameter(val type: ResolvedType, val mutable: kotlin.Boolean)
//        }
//    }

}