import eu.nitok.jitsu.compiler.ast.AstNode
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

fun range(it: eu.nitok.jitsu.compiler.parser.Range): Range {
    return Range(
        Position(it.start.line-1, it.start.column-1),
        Position(it.end.line-1, it.end.column-1)
    )
}

fun AstNode.visit(visitor: AstNode.() -> Unit) {
    val nodeQueue = ArrayDeque(listOf(this))
    while (!nodeQueue.isEmpty()) {
        val subNode = nodeQueue.removeFirst()
        visitor(subNode);
        nodeQueue += subNode.children
    }
}
