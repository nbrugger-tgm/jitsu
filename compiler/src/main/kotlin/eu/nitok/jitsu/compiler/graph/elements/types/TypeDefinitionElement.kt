package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Accessible
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware
import eu.nitok.jitsu.compiler.graph.elements.AccessibleElement
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface TypeDefinitionElement : Element, ModuleAware, AccessibleElement<TypeDefinition>, Accessible<TypeDefinition> {
    val asTypeDefinition: TypeDefinition
    override val name: Located<String>
}