package eu.nitok.jitsu.compiler.ast

import com.niton.parser.grammar.api.GrammarName

enum class CodeBlockContentType(val s: String) : GrammarName {
    STATEMENTS("CODE_BLOCK_STATEMENTS_BODY"),
    EXPRESSION("CODE_BLOCK_EXPRESSION_BODY");

    override fun getName(): String {
        return s
    }

    companion object {
        fun byGrammarName(it: String): GrammarName {
            return values().find { e -> e.s == it } ?: throw IllegalArgumentException("Unknown code block content type: $it")
        }
    }
}