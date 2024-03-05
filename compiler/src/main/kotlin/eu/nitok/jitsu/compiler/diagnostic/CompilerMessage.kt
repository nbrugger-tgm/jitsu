package eu.nitok.jitsu.compiler.diagnostic

import eu.nitok.jitsu.compiler.parser.Locatable
import kotlinx.serialization.Serializable

@Serializable
data class CompilerMessage(
    var message: String,
    val location: Locatable,
    val hints: List<Hint> = emptyList()
) {
    constructor(message: String, location: Locatable, vararg hints: Hint): this(message, location, hints.toList())
    @Serializable
    data class Hint(val message: String, val location: Locatable) {
        override fun toString(): String {
            return "$message ($location)"
        }
    }

    override fun toString(): String {
        return "$message ($location) ${
            if (hints.isNotEmpty()) "\n\tHints :\n\t\t" + hints.joinToString("\n\t\t") 
            else ""
        }"
    }
}