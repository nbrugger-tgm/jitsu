package eu.nitok.jitsu.compiler.ast

import com.niton.parser.grammar.api.GrammarName

enum class CaseMatchingType : GrammarName {
    DECONSTRUCT_PATTERN_MATCH, CASTING_PATTERN_MATCH;

    override fun getName(): String {
        return name
    }
}