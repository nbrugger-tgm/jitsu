import eu.nitok.jitsu.parser.ast.AstNode
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

fun range(it: eu.nitok.jitsu.common.Range): Range {
    return Range(
        Position(it.start.line-1, it.start.column-1),
        Position(it.end.line-1, it.end.column)
    )
}

fun range(it: eu.nitok.jitsu.common.Range, uri: String): Location {
    return Location(uri, range(it))
}
fun range(it: Range, uri: String): eu.nitok.jitsu.common.Range {
    return eu.nitok.jitsu.common.Range(location(it.start, uri),location(it.end, uri))
}

fun location(it: Position, uri: String? = null) = eu.nitok.jitsu.common.Location(it.line + 1, it.character + 1, uri)
fun location(it: eu.nitok.jitsu.common.Location) = Position(it.line - 1, it.column - 1)

fun range(it: Location): eu.nitok.jitsu.common.Range {
    return range(it.range, it.uri)
}
