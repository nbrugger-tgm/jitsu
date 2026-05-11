import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.net.URI

fun range(it: eu.nitok.jitsu.common.locating.Location): Range {
    return Range(
        Position(it.start.line-1, it.start.column-1),
        Position(it.end.line-1, it.end.column)
    )
}

fun range(it: eu.nitok.jitsu.common.locating.Location, uri: String): Location {
    return Location(uri, range(it))
}
fun range(it: Range, uri: String): eu.nitok.jitsu.common.locating.Location {
    return eu.nitok.jitsu.common.locating.Location(location(it.start, uri), location(it.end, uri))
}

fun location(it: Position, uri: String) =
    eu.nitok.jitsu.common.locating.Position(it.line + 1, it.character + 1, URI(uri))
fun location(it: eu.nitok.jitsu.common.locating.Position) = Position(it.line - 1, it.column - 1)

fun range(it: Location): eu.nitok.jitsu.common.locating.Location {
    return range(it.range, it.uri)
}
