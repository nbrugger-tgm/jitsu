package eu.nitok.jitsu.compiler.ast

import com.niton.jainparse.grammar.api.GrammarName

enum class CaseBodyType : GrammarName {
    CODE_BLOCK_CASE_BODY,
    EXPRESSION_CASE_BODY;

    override fun getName(): String {
        return name
    }
}