package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class TypeParameterElement(
    override val name: Located<String>
) : DirectTypeDefinitionElement(), TypeDefinition.DirectTypeDefinition.TypeParameter {
    @Transient
    override val children: List<Element> = emptyList()

    override fun toString(): String {
        return name.value
    }

    override fun rawType(
        resolveGeneric: ResolveGenericFn?
    ): TypeElement {
        return resolveGeneric?.let { it(this) }?: this
    }

    override fun accepts(type: Type): ReasonedBoolean {
        return run {
            if (type == this) ReasonedBoolean.True("$type is the same type as $this")
            else ReasonedBoolean.False("$type is not the same generic as $this")
        }
    }
}