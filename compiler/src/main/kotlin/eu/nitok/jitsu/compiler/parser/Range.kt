package eu.nitok.jitsu.compiler.parser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.max

@Serializable(with = Range.Serializer::class)
data class Range(val start: Location, val end: Location) : Locatable, Comparable<Range> {

    init {
        if(start.file != end.file) throw IllegalArgumentException("A range cannot span across files (${start.file} and ${end.file})")
    }

    companion object {
        val byStart: Comparator<Range> = Comparator.comparingInt { obj: Range -> obj.start.line }
            .thenComparingInt { obj: Range -> obj.start.column }
            .thenComparingInt { obj: Range -> obj.end.line }
            .thenComparingInt { obj: Range -> obj.end.column }

        val byEnd: Comparator<Range> = Comparator.comparingInt { obj: Range -> obj.end.line }
            .thenComparingInt { obj: Range -> obj.end.column }
            .thenComparingInt { obj: Range -> obj.start.line }
            .thenComparingInt { obj: Range -> obj.start.column }
    }

    override fun toRange(): Range {
        return this;
    }

    constructor(startCol: Int, startLine: Int, endCol: Int, endLine: Int) : this(
        Location(startCol, startLine),
        Location(endCol, endLine)
    )

    override fun format(): String = "${start.format()}-${end.format()}"

    override fun absoluteFormat(): String =
        if (start.file == null) format()
        else "${start.file}:${start.format()}-${end.format()}"


    override fun mark(text: String, note: String?): String {
        val lines = text.split("\n").toMutableList();
        val oneLine = start.line == end.line;
        if (oneLine) {
            lines.add(start.line, singleLineMark(note));
        } else {
            var startLine = lines[start.line - 1];
            var endLine = lines[end.line - 1];
            val vcol = max(startLine.length, endLine.length) + 3;
            startLine += "<${"-".repeat((vcol - startLine.length) - 2)}+${if (note != null) " $note" else ""}";
            endLine += "<${"-".repeat((vcol - endLine.length) - 1)}+";
            lines[end.line - 1] = endLine;
            lines[start.line - 1] = startLine;
            lines.subList(start.line, end.line - 1)
                .replaceAll { line -> line + " ".repeat(vcol - 1 - line.length) + "|" }
        }
        return lines.joinToString("\n" );
    }

    private fun singleLineMark(note: String?): String {
        val builder = StringBuilder();
        builder.append(" ".repeat(start.column - 1));
        builder.append("^");
        if(end.isAfter(start)) {
            val dashes = end.column - start.column - 1;
            val dashesWithNote = dashes - (note?.length ?: dashes);
            if (dashesWithNote >= 2) {
                builder.append("-".repeat(dashesWithNote / 2));
                builder.append(note);
                builder.append("-".repeat(dashesWithNote / 2));
            } else {
                builder.append("-".repeat(dashes));
            };
            builder.append("^");
            if(dashesWithNote<2) {
                builder.append(note);
            }
        } else {
            builder.append(" $note")
        }
        return builder.toString();
    }

    override fun toString(): String {
        return absoluteFormat();
    }

    override fun compareTo(other: Range): Int {
        return byStart.compare(this, other);
    }

    fun rangeTo(location: Location): Range {
        return Range(start, location)
    }

    fun rangeTo(range: Range): Range {
        return Range(start, range.end)
    }

    fun span(range: Range): Range {
        return Range(if (start.isBefore(range.start)) start else range.start, range.end)
    }
    object Serializer : KSerializer<Range> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("jitsu.Range", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Range) {
            encoder.encodeString(value.absoluteFormat())
        }

        override fun deserialize(decoder: Decoder): Range {
            val string = decoder.decodeString()
            val locs = string.split("-");
            var loc1 = run {
                val parts = locs[0].split(":")
                if (parts.size == 3) Location(
                    file = parts[0],
                    line = parts[1].toInt(),
                    column = parts[2].toInt()
                )
                else Location(
                    line = parts[0].toInt(),
                    column = parts[1].toInt()
                )
            }
            var loc2 = run {
                val parts = locs[1].split(":")
                Location(
                    file = loc1.file,
                    line = parts[0].toInt(),
                    column = parts[1].toInt()
                )
            }
            return Range(loc1, loc2)
        }
    }
}