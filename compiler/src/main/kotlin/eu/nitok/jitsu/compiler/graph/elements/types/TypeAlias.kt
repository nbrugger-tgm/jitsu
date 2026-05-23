package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeAware
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeProvider
import eu.nitok.jitsu.compiler.graph.elements.Scope
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class TypeAlias(
    override val name: Located<String>,
    override val generics: List<TypeParameterElement>,
    var typeElement: TypeElement
) : ParameterizedTypeElement(), TypeDefinition.ParameterizedType.Alias,
    ScopeProvider, ScopeAware  {
    override val type: Type get() = typeElement.asType
    override val children: List<Element> get() = listOf(type) + generics
    override fun toType(typeParameters: Map<String, TypeElement>): TypeElement {
        return typeElement.rawType { generic ->
            typeParameters[generic.name.value]
                ?: throw IllegalArgumentException("Its the compiler developers job that toType(generics) has a complete generics map. ${generic.name.value} was absent in map $typeParameters")
        }
    }

    @Transient
    override val scope: Scope = Scope(typeElements = generics.associateBy { it.name.value })

    override fun setEnclosingScope(parent: Scope) {
        scope.parent = parent
    }

    override fun toString(): String {
        return "${name.value}${if (generics.isNotEmpty()) "<${generics.joinToString(", ")}>" else ""}"
    }
}