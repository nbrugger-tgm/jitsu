package eu.nitok.jitsu.compiler.ast

import eu.nitok.jitsu.compiler.parser.Range

class SourceFileNode(val statements: List<StatementNode>) : AstNodeImpl() {
    override val location: Range
        get() = if(statements.isEmpty()) Location(0,0) else statements.first().location.rangeTo(statements.last().location)
}