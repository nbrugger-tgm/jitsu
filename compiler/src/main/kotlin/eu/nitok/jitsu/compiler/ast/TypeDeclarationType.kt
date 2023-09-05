package eu.nitok.jitsu.compiler.ast

import com.niton.parser.grammar.api.GrammarName

enum class TypeDeclarationType : GrammarName {
    ARRAY_TYPE,
    FIXED_VALUE_TYPE,
    NAMED_TYPE,
    UNION_TYPE,
    ENUM_TYPE;

    override fun getName(): String {
        return name
    }
}