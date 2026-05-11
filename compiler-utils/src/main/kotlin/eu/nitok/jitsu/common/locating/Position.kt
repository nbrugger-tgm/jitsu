package eu.nitok.jitsu.common.locating

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.readText

@Serializable(with = Position.Serializer::class)
data class Position(val line: Int, val column: Int, var file: URI) : Locatable {
    override fun format() = "$line:$column"

    override fun absoluteFormat(): String {
        return "$file:${format()}"
    }

    override fun absolutePositionFormat(): String {
        return absoluteFormat()
    }

    override fun mark(note: String): String {
        val fileContent = try {
            //TODO: content should be passed and not be re-read so often in here
            // but that requires the GRAPH to have a "parent" system to find the origin file of any Element
            file.toURL().readText()
        } catch (e: IOException){null}
        if(fileContent == null) return "${file}:${line} | $note"
        val lines = fileContent.split("\n").toMutableList()
        val pointerLine = "${" ".repeat(column - 1)}^ ($note)"
        lines.add(this.line, pointerLine)
        return lines.joinToString("\n")
    }

    override fun toLocation(): Location {
        return Location(this, this)
    }

    override fun toString() = absoluteFormat()

    fun rangeTo(position: Position): Location {
        return Location(this, position)
    }

    fun isBefore(other: Position): Boolean {
        return line < other.line || (line == other.line && column < other.column)
    }

    fun isAfter(other: Position): Boolean {
        return line > other.line || (line == other.line && column > other.column)
    }

    object Serializer : KSerializer<Position> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("jitsu.Position", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Position) {
            encoder.encodeString(value.absoluteFormat())
        }

        override fun deserialize(decoder: Decoder): Position {
            val string = decoder.decodeString()
            return deserialize(string)
        }

        fun deserialize(string: String): Position {
            val parts = string.split(":")
            return if (parts.size >= 3) Position(
                file = URI(parts.subList(0, parts.size - 2).joinToString(":")),
                line = parts[parts.size - 2].toInt(),
                column = parts[parts.size - 1].toInt()
            )
            else throw SerializationException("Unknown location format: $string")
        }
    }
}
