package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.Serializable


@Serializable
data class AttributeNodeImpl(
    var name: IdentifierNode,
    var values: List<AttributeValueNode>,
    override val location: Location
) : AstNodeImpl() {
    @Serializable
    data class AttributeValueNode(
        var name: IdentifierNode,
        var value: ExpressionNode?,
    ) : AstNodeImpl() {
        override val location: Location
            get() = value?.let { name.location.rangeTo(it.location) } ?: name.location
    }
}
