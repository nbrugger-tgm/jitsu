package eu.nitok.jitsu.compiler.parser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = Location.Serializer::class)
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

    override fun toRange(): Range {
        return Range(this, this)
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

    object Serializer : KSerializer<Location> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("jitsu.Location", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Location) {
            encoder.encodeString(value.absoluteFormat())
        }

        override fun deserialize(decoder: Decoder): Location {
            val string = decoder.decodeString()
            val parts = string.split(":")
            return if (parts.size == 3) Location(
                file = parts[0],
                line = parts[1].toInt(),
                column = parts[2].toInt()
            )
            else Location(
                line = parts[1].toInt(),
                column = parts[2].toInt()
            )
        }
    }
}
