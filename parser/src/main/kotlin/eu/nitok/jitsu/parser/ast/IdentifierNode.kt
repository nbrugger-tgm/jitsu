package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.locating.Location
import kotlinx.serialization.Serializable

class IdentifierNode(
    override val location: Location,
    val value: String
) : AstNodeImpl() {
    override val children: List<AstNode>
        get() = listOf()
    override fun toString() = value
}
