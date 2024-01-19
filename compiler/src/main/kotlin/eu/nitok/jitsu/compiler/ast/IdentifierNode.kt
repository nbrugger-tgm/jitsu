package eu.nitok.jitsu.compiler.ast

import kotlinx.serialization.Serializable

@Serializable
class IdentifierNode(
    override val location: Location,
    override val value: String
) : AstNodeImpl(), Located<String>
