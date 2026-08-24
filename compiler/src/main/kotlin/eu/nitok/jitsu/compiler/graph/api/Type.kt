package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.ParameterizedType.*
import kotlinx.serialization.Contextual

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

    interface Value : Type {
        val value: Expression.Constant<@Contextual Any>
    }


    interface Array : Type {
        val elementType: Type
        val size: Expression.Constant.UIntConstant?
        val sizeType: Type
    }

    interface FunctionSignature : Type, Function.Signature

    interface TypeReference : Type, Access.TypeAccess
    interface ParameterizedTypeReference<S>: Type, AccessWithParameters<TypeDefinition.ParameterizedType, S>
            where S : Specialized<TypeDefinition.ParameterizedType>,
                  S : Type

    interface Union : Type {
        val options: List<Type>
    }

    interface StructuralInterface : Type {
        val fields: Map<String, TypeStructure.Struct.Field>
    }

    interface Class : Type, Specialized<ClassDefinition>, TypeStructure.Class
    interface Interface : Type, Specialized<InterfaceDefinition>, TypeStructure.Interface
    interface Struct : Type, Specialized<StructDefinition>, TypeStructure.Struct
}