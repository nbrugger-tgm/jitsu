package eu.nitok.jitsu.common.locating

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.IOException
import java.net.URI
import kotlin.math.max

@Serializable(Location.Serializer::class)
data class Location(val start: Position, val end: Position) : Locatable, Comparable<Location> {

    init {
        if(start.file != end.file) throw IllegalArgumentException("A range cannot span across files (${start.file} and ${end.file})")
    }

    companion object {
        val byStart: Comparator<Location> = Comparator.comparingInt { obj: Location -> obj.start.line }
            .thenComparingInt { obj: Location -> obj.start.column }
            .thenComparingInt { obj: Location -> obj.end.line }
            .thenComparingInt { obj: Location -> obj.end.column }

        val byEnd: Comparator<Location> = Comparator.comparingInt { obj: Location -> obj.end.line }
            .thenComparingInt { obj: Location -> obj.end.column }
            .thenComparingInt { obj: Location -> obj.start.line }
            .thenComparingInt { obj: Location -> obj.start.column }
    }

    override fun toLocation(): Location {
        return this;
    }

    constructor(file: URI, startCol: Int, startLine: Int, endCol: Int, endLine: Int) : this(
        Position(startLine, startCol, file),
        Position(endLine, endCol, file)
    )

    override fun format(): String = "${start.format()}-${end.format()}"

    override fun absoluteFormat(): String = "${start.absoluteFormat()}-${end.format()}"
    override fun absolutePositionFormat(): String {
        return start.absoluteFormat()
    }


    override fun mark(note: String): String {
        val fileContent = try {
            start.file.toURL().readText()
        } catch (e: IOException) { null }
        if(fileContent == null) return "${start.absoluteFormat()} : $note"
        val lines = fileContent.split("\n").toMutableList();
        val oneLine = start.line == end.line;
        if (oneLine) {
            lines.add(start.line, singleLineMark(note));
        } else {
            var startLine = lines[start.line - 1]
            var endLine = lines[end.line - 1]
            val noteColumn = max(startLine.length, endLine.length) + 3
            startLine += "<${"-".repeat((noteColumn - startLine.length) - 1)}+${" $note"}"
            endLine += "<${"-".repeat((noteColumn - endLine.length) - 1)}+"
            lines[end.line - 1] = endLine
            lines[start.line - 1] = startLine
            lines.subList(start.line, end.line - 1)
                .replaceAll { line -> "$line${" ".repeat(noteColumn - 1 - line.length)}|" }
        }
        return lines.subList(maxOf(start.line-3,0), minOf(end.line+3, lines.size-1)).joinToString("\n" );
    }

    private fun singleLineMark(note: String): String {
        val builder = StringBuilder()
        builder.append(" ".repeat(start.column - 1))
        builder.append("^")
        if(end.isAfter(start)) {
            val dashes = end.column - start.column - 1
            val dashesWithNote = dashes - note.length
            if (dashesWithNote >= 2) {
                builder.append("-".repeat(dashesWithNote / 2))
                builder.append(note)
                builder.append("-".repeat(dashesWithNote / 2))
            } else {
                builder.append("-".repeat(dashes))
            };
            builder.append("^")
            if(dashesWithNote<2) {
                builder.append(note)
            }
        } else {
            builder.append(" $note")
        }
        return builder.toString()
    }

    override fun toString(): String {
        return absoluteFormat()
    }

    override fun compareTo(other: Location): Int {
        return byStart.compare(this, other)
    }

    fun rangeTo(position: Position): Location {
        return Location(start, position)
    }

    fun rangeTo(position: Locatable): Location {
        return when(position) {
            is Location -> rangeTo(position)
            is Position -> rangeTo(position)
        }
    }

    fun rangeTo(position: HasLocation): Location {
        return rangeTo(position.location)
    }

    fun rangeTo(location: Location): Location {
        return Location(start, location.end)
    }

    fun span(location: Location): Location {
        return Location(if (start.isBefore(location.start)) start else location.start, location.end)
    }

    fun contains(position: Position): Boolean {
        return position.file == start.file && !position.isBefore(start) && !position.isAfter(end.copy(column = end.column + 1))
    }


    object Serializer : KSerializer<Location> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("jitsu.Location", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Location) {
            encoder.encodeString(value.absoluteFormat())
        }

        override fun deserialize(decoder: Decoder): Location {
            val string = decoder.decodeString()
            return deserialize(string)
        }

        fun deserialize(string: String): Location {
            val dashIndex = string.indexOfLast { it == '-' };
            if (dashIndex == -1) throw SerializationException("Invalid location format: $string")
            val from = string.substring(0, dashIndex)
            val to = string.substring(dashIndex + 1)
            val loc1 = run {
                val parts = from.split(":")
                if (parts.size >= 3) Position(
                    file = URI(parts.subList(0, parts.size - 2).joinToString(":")),
                    line = parts[parts.size - 2].toInt(),
                    column = parts[parts.size - 1].toInt()
                ) else throw SerializationException("Invalid location format: $string")
            }
            val loc2 = run {
                val parts = to.split(":")
                Position(
                    file = loc1.file,
                    line = parts[0].toInt(),
                    column = parts[1].toInt()
                )
            }
            return Location(loc1, loc2)
        }
    }
}