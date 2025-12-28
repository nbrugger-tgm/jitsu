package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Serializable

@Serializable
class IdentifierNode(
    override val location: Range,
    val value: String
) : AstNodeImpl() {
    override val children: List<AstNode>
        get() = listOf()
    override fun toString() = value
}
