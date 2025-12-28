package eu.nitok.jitsu.compiler.model

enum class BiOperator(val rune: String,val functionName: String) {
    GREATER_EQUAL(">=", "greater_or_equal_to"),
    LESS_EQUAL("<=", "less_or_equal_to"),
    AND("&&","logical_and"),
    OR("||","logical_or"),
    ADDITION("+","plus"),
    SUBTRACTION("-","minus"),
    MULTIPLICATION("*","multiply_by"),
    DIVISION("/","divide_by"),
    MODULO("%","modulo"),
    GREATER(">","greater_than"),
    LESS("<","less_than"),
    EQUALS("==","equals");

    companion object {
        fun byRune(rune: String): BiOperator? {
            return entries.find { it.rune == rune }
        }
    }
}