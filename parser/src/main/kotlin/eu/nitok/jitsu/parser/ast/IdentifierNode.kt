package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.Range
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
