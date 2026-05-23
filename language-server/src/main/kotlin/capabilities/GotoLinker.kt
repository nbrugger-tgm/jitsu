package capabilities

import eu.nitok.jitsu.common.locating.Position
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.graph.api.Access
import eu.nitok.jitsu.compiler.graph.api.JitsuFile
import location
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.jsonrpc.messages.Either

fun JitsuFile.findDefinition(reference: Position): Either<MutableList<out Location>, MutableList<out LocationLink>>? {
    val reference = sequence()
        .filterIsInstance<Access<*>>()
        .find { it.reference.location.contains(reference) }
    val declaration = reference?.target?.name?.location?.let { location(it) }
    return Either.forLeft(listOfNotNull(declaration).toMutableList())
}