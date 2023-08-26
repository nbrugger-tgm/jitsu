package eu.nitok.jitsu.compiler.model

import com.niton.parser.grammar.api.GrammarName

enum class LiteralType : GrammarName {
    NUMBER_LITERAL, VARIABLE_LITERAL;

    override fun getName(): String {
        return name;
    }
}