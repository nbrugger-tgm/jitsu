package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition
import eu.nitok.jitsu.compiler.graph.elements.NamedFunctionSignature
import kotlinx.serialization.Serializable

@Serializable
internal data class Interface(
    override val name: Located<String>,
    override val generics: List<TypeParameterElement>,
    override val methods: Map<String, List<NamedFunctionSignature>>
) : ParameterizedTypeElement(), TypeDefinition.ParameterizedType.Interface {
    constructor(
        name: Located<String>,
        generics: List<TypeParameterElement>,
        methods: List<NamedFunctionSignature>
    ) : this(name, generics, methods.groupBy { it.name.value })

    override val children: List<Element> get() = methods.values.flatten()
    override fun toType(
        typeParameters: Map<String, TypeElement>
    ): TypeElement {
        TODO("Not yet implemented")
    }
}