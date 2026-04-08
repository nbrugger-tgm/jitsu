package eu.nitok.jitsu.common

fun indent(stages: Int, joinToString: String): String {
    return joinToString.lines().joinToString("\n") { "    ".repeat(stages) + it }
}