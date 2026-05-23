package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class FunctionTypeSignature(
    val returnTypeElement: TypeElement?,
    override val parameters: List<Parameter>
) : TypeElement(), Type.FunctionTypeSignature {
    override val returnType: Type? get() = returnTypeElement?.asType
    override fun rawType(resolveGeneric: ResolveGenericFn?): TypeElement {
        return FunctionTypeSignature(
            returnTypeElement?.rawType(resolveGeneric),
            parameters.map { it.copy(typeElement = it.typeElement.rawType(resolveGeneric)) })
    }

    override fun accepts(type: Type): ReasonedBoolean {
        TODO("Not yet implemented")
    }

    @Transient
    override val children: List<Element> = parameters + listOfNotNull(returnType)

    override fun toString(): String {
        return "(${parameters.joinToString(", ") { "${it.type}" }}) -> ${returnType ?: "void"}"
    }

    @Serializable
    internal data class Parameter(
        override val name: Located<String>,
        val typeElement: TypeElement,
        override var optional: kotlin.Boolean
    ) : Type.FunctionTypeSignature.Parameter {

        override val type: Type get() = typeElement.asType

        @Transient
        override val children: List<Element> = listOf(type)
        override fun toString(): String {
            return "$name: $type"
        }
    }
}