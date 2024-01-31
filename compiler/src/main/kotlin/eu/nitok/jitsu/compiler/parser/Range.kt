package eu.nitok.jitsu.compiler.parser

data class Range(val start: Location, val end: Location) : Locatable {
    override fun toString(): String {
        return if (start.file == null) "$start-$end" else "${start.file}:${start.charRefereneString()}-${end.charRefereneString()}";
    }

    fun rangeTo(location: Location): Range {
        return Range(start, location)
    }

    fun rangeTo(range: Range): Range {
        return Range(start, range.end)
    }
}