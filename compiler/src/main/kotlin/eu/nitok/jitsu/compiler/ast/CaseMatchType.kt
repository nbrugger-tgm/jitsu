package eu.nitok.jitsu.compiler.ast

import com.niton.parser.grammar.api.GrammarName

enum class CaseMatchType : GrammarName {
    CONSTANT_CASE,
    CONDITION_CASE,
    DEFAULT_CASE;

    override fun getName(): String {
        return name
    }
}