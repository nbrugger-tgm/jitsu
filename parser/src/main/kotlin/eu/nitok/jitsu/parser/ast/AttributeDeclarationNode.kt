package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.locating.Location

data class AttributeDeclarationNode(
    val name: IdentifierNode?,
    val properties: List<AttributeProperty>,
    val kwLocation: Location,
    val openKwLocation: Location?,
    val closeKwLocation: Location?,
    override val attributes: List<AttributeNode>,
) : AstNodeImpl(), StatementNode.Declaration, CanHaveAttributes {
    data class AttributeProperty(
        val name: IdentifierNode?,
        val type: TypeNode?
    ) : AstNodeImpl() {
        init {
            require(name != null || type != null) { "At least one property must be defined" }
        }
        override val location: Location get() = if(name != null && type != null) name.location.rangeTo(type.location)
        else name?.location ?: type!!.location
        override val children: List<AstNode> get() = listOfNotNull(type)
    }

    override val location: Location get() = kwLocation.rangeTo(closeKwLocation?:properties.lastOrNull()?.location?:openKwLocation?:name?.location?:kwLocation)
    override val children: List<AstNode> get() = properties + attributes
}