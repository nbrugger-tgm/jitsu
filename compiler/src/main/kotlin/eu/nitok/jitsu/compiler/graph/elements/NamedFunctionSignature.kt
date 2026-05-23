package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.NamedFunctionSignature
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.elements.types.FunctionTypeSignature
import kotlinx.serialization.Serializable

@Serializable
internal class NamedFunctionSignature(
    override val name: Located<String>,
    override val typeSignature: FunctionTypeSignature
) : NamedFunctionSignature {
    override val children: List<Element>
        get() = listOf(typeSignature)
}