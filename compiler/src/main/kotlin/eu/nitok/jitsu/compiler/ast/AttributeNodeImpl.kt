package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Serializable


@Serializable
data class AttributeNodeImpl(
    var name: IdentifierNode,
    var values: List<AttributeValueNode>,
    override val location: Range
) : AstNodeImpl() {
    @Serializable
    data class AttributeValueNode(
        var name: IdentifierNode,
        var value: ExpressionNode?,
    ) : AstNodeImpl() {
        override val location: Range
            get() = value?.let { name.location.rangeTo(it.location) } ?: name.location
    }
}
