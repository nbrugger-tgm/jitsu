package eu.nitok.jitsu.compiler.graph.elements.types

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware
import eu.nitok.jitsu.compiler.graph.elements.SymbolElement
import kotlinx.serialization.Serializable

/**
 * Common supertype of all type definition elements.
 *
 * _Implementation note_: This is intentionally not generic over the [TypeDefinition] it represents. Star projecting the
 * self referential `TypeDefinition<T : TypeDefinition<T>>` inside a serializable property makes the kotlinx.serialization
 * compiler plugin (1.9.x) recurse infinitely. Subtypes bind the concrete definition type via
 * [eu.nitok.jitsu.compiler.graph.elements.AccessibleElement].
 */
@Serializable
internal sealed interface TypeDefinitionElement : Element, SymbolElement {
    val asTypeDefinition: TypeDefinition<*>
    val name: Located<String>
}
