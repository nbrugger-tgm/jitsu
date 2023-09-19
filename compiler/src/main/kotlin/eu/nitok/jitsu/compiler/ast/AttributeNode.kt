package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.Serializable


@Serializable
data class AttributeNode(
    var name: N<Located<String>>,
    var values: List<N<AttributeValueNode>>,
    var location: Location
) {
    @Serializable
    data class AttributeValueNode(
        var name: N<Located<String>>,
        var value: N<ExpressionNode>,
    ) {
        val location: Location
            get() = com.niton.parser.token.Location.range(
                name.location,
                value.location { it.location }
            )
    }
}
