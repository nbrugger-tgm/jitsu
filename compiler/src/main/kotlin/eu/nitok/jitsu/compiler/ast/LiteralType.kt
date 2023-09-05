package eu.nitok.jitsu.compiler.ast

import com.niton.parser.grammar.api.GrammarName

enum class LiteralType : GrammarName {
    NUMBER_LITERAL, VARIABLE_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL;

    override fun getName(): String {
        return name;
    }
}