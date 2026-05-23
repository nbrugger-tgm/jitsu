package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.ParameterizedType.Struct
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

sealed interface Type : Element {
    /**
     * The type but with all [type references][TypeReference] resolved. Can still contain [generics][TypeDefinition.DirectTypeDefinition.TypeParameter]
     */
    val rawType: Type
    fun acceptsInstanceOf(type: Type): ReasonedBoolean

    sealed interface Primitive : Type {
        val size: BitSize
    }

    interface Int : Primitive
    interface UInt : Primitive
    interface Float : Primitive
    interface Boolean : Primitive

    interface Null : Type
    /**
     * This type is not usable in the language. It is the type used at compile time when a type is not resolvable/errornous
     */
    interface Undefined : Type

    interface Value: Type {
        val value: Expression.Constant<@Contextual Any>
    }


    interface Array : Type {
        val elementType: Type
        val size: Expression.Constant.IntConstant?
        val sizeType: Type
    }

    interface FunctionTypeSignature : Type {
        val returnType: Type?
        val parameters: List<Parameter>

        interface Parameter : Element {
            val name: Located<String>
            val type: Type
            val optional: kotlin.Boolean
        }
    }

    interface TypeReference : Type, Access.TypeAccess {
        val genericParameters: List<Located<Type>>
    }

    interface Union : Type {
        val options: List<Type>
    }

    interface StructuralInterface : Type {
        val fields: Map<String, Struct.Field>
    }
}