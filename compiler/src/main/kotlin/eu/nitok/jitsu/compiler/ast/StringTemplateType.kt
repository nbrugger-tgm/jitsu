package eu.nitok.jitsu.compiler.ast

import com.niton.jainparse.grammar.api.GrammarName

enum class StringTemplateType : GrammarName {
    TEMPLATE_LITERAL,
    TEMPLATE_EXPRESSION;

    override fun getName(): String {
        return name
    }
}