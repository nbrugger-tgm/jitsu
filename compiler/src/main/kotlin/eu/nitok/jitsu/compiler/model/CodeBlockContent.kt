package eu.nitok.jitsu.compiler.model

import com.niton.parser.grammar.api.GrammarName

enum class CodeBlockContent(val s: String) : GrammarName {
    STATEMENTS("CODE_BLOCK_STATEMENTS_BODY"),
    EXPRESSION("CODE_BLOCK_EXPRESSION_BODY");

    override fun getName(): String {
        return s
    }
}