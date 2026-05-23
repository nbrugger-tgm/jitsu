package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import kotlinx.serialization.Serializable

@Serializable
internal data class Class(
    override val name: Located<String>,
    override val generics: List<TypeParameterElement>,
    override val fields: List<Struct.Field>,
    override val methods: List<FunctionElement>
) : ParameterizedTypeElement(), TypeDefinition.ParameterizedType.Class {
    override val children: List<Element>
        get() = fields + methods // + generics

    override fun toType(
        typeParameters: Map<String, TypeElement>
    ): TypeElement {
        TODO("Not yet implemented")
    }

}