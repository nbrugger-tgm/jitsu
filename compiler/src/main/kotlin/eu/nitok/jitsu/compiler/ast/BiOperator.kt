package eu.nitok.jitsu.compiler.ast

import com.niton.jainparse.grammar.api.GrammarName

enum class BiOperator(val rune: String) : GrammarName {
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

    override fun getName(): String {
        return name
    }
}