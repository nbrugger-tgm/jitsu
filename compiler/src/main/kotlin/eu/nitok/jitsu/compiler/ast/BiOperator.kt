package eu.nitok.jitsu.compiler.ast

enum class BiOperator(val rune: String) {
    GREATER_EQUAL(">="),
    LESS_EQUAL("<="),
    AND("&&"),
    OR("||"),
    ADDITION("+"),
    SUBTRACTION("-"),
    MULTIPLICATION("*"),
    DIVISION("/"),
    MODULO("%"),
    GREATER(">"),
    LESS("<");

    companion object {
        fun byRune(rune: String): BiOperator? {
            return entries.find { it.rune == rune }
        }
    }
}