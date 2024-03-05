package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.parser.Range
import kotlinx.serialization.Serializable

data class SourceFileNode(
    val statements: List<StatementNode>
) : AstNodeImpl(statements) {
    override val location: Range
        get() = if(statements.isEmpty()) Range(0,0,0,0) else statements.first().location.span(statements.last().location)
}