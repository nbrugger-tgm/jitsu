import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.lang.System.getProperty
import java.net.URI
import java.util.Locale.getDefault


fun location(it: eu.nitok.jitsu.common.locating.Location): Location {
    return Location(it.start.file.toString(), range(it))
}

fun range(location: eu.nitok.jitsu.common.locating.Location): Range = Range(
    Position(location.start.line - 1, location.start.column - 1),
    Position(location.end.line - 1, location.end.column)
)

fun location(it: Range, uri: String): eu.nitok.jitsu.common.locating.Location {
    return eu.nitok.jitsu.common.locating.Location(location(it.start, uri), location(it.end, uri))
}

fun location(it: Position, uri: String) =
    eu.nitok.jitsu.common.locating.Position(it.line + 1, it.character + 1, URI(uri))
fun location(it: eu.nitok.jitsu.common.locating.Position) = Position(it.line - 1, it.column - 1)

fun location(it: Location): eu.nitok.jitsu.common.locating.Location {
    return location(it.range, it.uri)
}

fun isWindows(): Boolean = getProperty("os.name").lowercase(getDefault()).contains("windows")