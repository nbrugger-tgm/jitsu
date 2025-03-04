package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
data class AttributeNode(
    var name: IdentifierNode,
    var values: List<AttributeValueNode>,
    override val location: Range
) : AstNodeImpl() {
    override val children: List<AstNode> get() = values + name
    @Serializable
    data class AttributeValueNode(
        var name: IdentifierNode,
        var value: ExpressionNode?,
    ) : AstNodeImpl() {
        override val children: List<AstNode> get() = listOfNotNull(value, name)
        override val location: Range
            get() = value?.let { name.location.rangeTo(it.location) } ?: name.location
    }
}
