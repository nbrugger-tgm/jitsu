package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.locating.Location
import kotlinx.serialization.Serializable


data class AttributeNode(
    val name: IdentifierNode?,
    val values: List<AttributeValueNode>,
    val openKw: Location,
    val closeKw: Location?,
) : AstNodeImpl() {
    override val location: Location = if(closeKw != null) openKw.rangeTo(closeKw)
    else if(name != null) openKw.rangeTo(name)
    else openKw
    override val children: List<AstNode> get() = values + listOfNotNull(name)
        data class AttributeValueNode(
        var name: IdentifierNode,
        var value: ExpressionNode?,
    ) : AstNodeImpl() {
        override val children: List<AstNode> get() = listOfNotNull(value, name)
        override val location: Location
            get() = value?.let { name.location.rangeTo(it.location) } ?: name.location
    }
}
