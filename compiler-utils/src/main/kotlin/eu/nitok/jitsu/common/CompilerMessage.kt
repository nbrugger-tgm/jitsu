package eu.nitok.jitsu.common

import eu.nitok.jitsu.common.locating.HasLocation
import eu.nitok.jitsu.common.locating.Locatable
import kotlinx.serialization.Serializable

data class CompilerMessage(
    var message: String,
    val location: Locatable,
    val hints: List<Hint> = emptyList()
) {
    constructor(message: String, location: Locatable, vararg hints: Hint): this(message, location, hints.toList())
    constructor(message: String, location: HasLocation, vararg hints: Hint): this(message, location.location, hints.toList())

    @Serializable
    data class Hint(val message: String, val location: Locatable) {
        constructor(message: String, location: HasLocation): this(message, location.location)
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

fun CompilerMessage.format(type: String): String {
    val errorMark = this.location.mark(this.message)
    return "$type: ${this.location.absolutePositionFormat()} ${this.message}\n" + errorMark + this.hints.joinToString("\t----\n") {
        val hintMark = it.location.mark(it.message).replace(Regex("^", RegexOption.MULTILINE), "\t| ")
        hintMark
    }
}