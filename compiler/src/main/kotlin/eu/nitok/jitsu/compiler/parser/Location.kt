package eu.nitok.jitsu.compiler.parser

import kotlinx.serialization.Serializable

@Serializable
data class Location(val line: Int, val column: Int, var file: String? = null) : Locatable {
    override fun format() = "$line:$column"

    override fun absoluteFormat(): String {
        return if (file != null) {
            "$file:${format()}"
        } else {
            format()
        }
    }

    override fun mark(text: String, note: String?): String {
        val lines = text.split("\n").toMutableList()
        val pointer = " ".repeat(column - 1) + "^"
        val noteString = if (note != null) " ($note)" else ""
        lines.add(this.line, pointer + noteString)
        return lines.joinToString("\n")
    }

    override fun toString() = absoluteFormat()

    fun rangeTo(location: Location): Range {
        return Range(this, location)
    }

    fun isBefore(other: Location): Boolean {
        return line < other.line || (line == other.line && column < other.column)
    }

    fun isAfter(other: Location): Boolean {
        return line > other.line || (line == other.line && column > other.column)
    }
}
