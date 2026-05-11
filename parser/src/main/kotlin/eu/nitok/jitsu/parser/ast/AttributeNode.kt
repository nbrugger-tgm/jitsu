package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.locating.Location
import kotlinx.serialization.Serializable


data class AttributeNode(
    var name: IdentifierNode,
    var values: List<AttributeValueNode>,
    override val location: Location
) : AstNodeImpl() {
    override val children: List<AstNode> get() = values + name
        data class AttributeValueNode(
        var name: IdentifierNode,
        var value: ExpressionNode?,
    ) : AstNodeImpl() {
        override val children: List<AstNode> get() = listOfNotNull(value, name)
        override val location: Location
            get() = value?.let { name.location.rangeTo(it.location) } ?: name.location
    }
}
