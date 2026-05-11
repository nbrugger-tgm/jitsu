package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.locating.Location
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.nio.file.Path


class SourceFileNode(
    val url: URI,
    val statements: List<StatementNode>,
) : AstNodeImpl() {

    override val children: List<AstNode>
        get() = statements
    override val location: Location
        get() = if(statements.isEmpty()) Location(url,0,0,0,0) else statements.first().location.span(statements.last().location)
}
