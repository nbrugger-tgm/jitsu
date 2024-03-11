package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.Located
import kotlinx.serialization.Serializable

@Serializable
class NamedFunctionSignature(val name: Located<String>, val typeSignature: Type.FunctionTypeSignature) : Element {
    override val children: List<Element>
        get() = listOf(typeSignature)
}