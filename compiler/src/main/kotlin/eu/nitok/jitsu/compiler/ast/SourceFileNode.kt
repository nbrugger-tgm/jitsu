package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class SourceFileNode(
    val url: String,
    val statements: List<StatementNode>
) : AstNodeImpl() {
    override val children: List<AstNode>
        get() = statements
    override val location: Range
        get() = if(statements.isEmpty()) Range(0,0,0,0) else statements.first().location.span(statements.last().location)
}