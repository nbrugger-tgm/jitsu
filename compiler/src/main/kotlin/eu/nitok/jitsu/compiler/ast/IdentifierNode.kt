package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Serializable

@Serializable
class IdentifierNode(
    override val location: Range,
    override val value: String
) : AstNodeImpl(listOf()), Located<String> {
    override fun toString() = value
}
