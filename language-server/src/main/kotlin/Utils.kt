import eu.nitok.jitsu.compiler.ast.AstNode
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

fun range(it: eu.nitok.jitsu.compiler.parser.Range): Range {
    return Range(
        Position(it.start.line, it.start.column),
        Position(it.end.line, it.end.column)
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

fun <T> AstNode.flatMap(mapper: (AstNode) -> List<T>): List<T> {
    val diagnostics: MutableList<T> = mutableListOf();
    val nodeQueue = ArrayDeque(listOf(this))
    while (!nodeQueue.isEmpty()) {
        val subNode = nodeQueue.removeFirst()
        diagnostics.addAll(mapper(subNode))
        nodeQueue += subNode.children
    }
    return diagnostics
}
