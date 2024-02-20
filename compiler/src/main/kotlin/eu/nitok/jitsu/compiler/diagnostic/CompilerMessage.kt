package eu.nitok.jitsu.compiler.diagnostic

import eu.nitok.jitsu.compiler.parser.Locatable
import kotlinx.serialization.Serializable

@Serializable
class CompilerMessage(
    var message: String,
    val location: Locatable,
    val hints: List<Hint> = emptyList()
) {
    @Serializable
    data class Hint(val message: String, val location: Locatable)
}