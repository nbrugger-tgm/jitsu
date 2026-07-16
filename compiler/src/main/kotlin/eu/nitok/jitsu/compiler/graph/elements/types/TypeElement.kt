package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable


internal typealias ResolveGenericFn = (TypeParameterElement) -> TypeElement

@Serializable
internal sealed class TypeElement: Element {
    /**
     * Reduces the type to its "raw" form by resolving all [references][TypeReference] from the graph and substituting GenericTypes/Type parameters when required
     *
     * _Implementation Note_: If no generics are being substituted the result should be cached in [rawType]/[rawTypeElement]
     *
     * @param resolveGeneric substitution function for [generics][TypeParameterElement]. If null no substitution will happen, if set the type returned by the function will be used
     */
    internal abstract fun rawType(resolveGeneric: ResolveGenericFn? = null): TypeElement

    val rawType: Type by lazy { rawTypeElement.asType }

    val rawTypeElement: TypeElement by lazy { rawType() }

    val asType: Type by lazy {
        return@lazy when(this) {
            Boolean -> Boolean
            Null -> Null
            Undefined -> Undefined
            is Array -> this
            is Float -> this
            is FunctionTypeSignature -> this
            is Int -> this
            is StructuralInterface -> this
            is TypeReference -> this
            is UInt -> this
            is Union -> this
            is Enum -> this
            is TypeParameterElement -> this
            is Value -> this
        }
    }

    fun acceptsInstanceOf(type: Type): ReasonedBoolean {
        return if (type is Undefined)
            ReasonedBoolean.True("While UNDEFINED cannot be assigned to anything, the error lies in the definition of the type not its usage")
        else if (type is Union) {
            val optionAssignability = type.options.map { mapAssignabilityBoolean(acceptsInstanceOf(it), it, this) }
            if (optionAssignability.all { boolean -> boolean.value }) ReasonedBoolean.True(
                "Each type in the union is assignable to $this",
                *optionAssignability.toTypedArray()
            )
            else {
                val assignWholeUnion = accepts(type)
                if (assignWholeUnion.value) return assignWholeUnion
                ReasonedBoolean.False(
                    "Not all types in the union ($type), nor the union itself are/is assignable to $this",
                    *(optionAssignability.filter { !it.value } + assignWholeUnion).toTypedArray()
                )
            }
        } else if (type is Type.TypeReference) {
            acceptsInstanceOf(type.rawType)
        } else {
            val reason = accepts(type)
            if (!reason.value) ReasonedBoolean.False("$type not assignable to $this", reason)
            else reason
        }
    }

    abstract fun accepts(type: Type): ReasonedBoolean
}
private fun mapAssignabilityBoolean(boolean: ReasonedBoolean, from: Type, to: TypeElement): ReasonedBoolean {
    return if (boolean.value) ReasonedBoolean.True("$from is assignable to $to", boolean)
    else ReasonedBoolean.False("$from is not assignable to $to", boolean)
}
