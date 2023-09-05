package eu.nitok.jitsu.compiler.ast

import com.niton.parser.grammar.api.GrammarName

enum class StatementType : GrammarName {
    VARIABLE_DECLARATION,
    FUNCTION_DECLARATION,
    FUNCTION_CALL,
    METHOD_INVOCATION,
    SEMICOLON_STATEMENT,
    RETURN_STATEMENT,
    ASSIGNMENT,
    IF_STATEMENT,
    CODE_BLOCK,
    SWITCH_STATEMENT,
    YIELD_STATEMENT,
    TYPE_DEFINITION;

    override fun getName(): String {
        return this.name
    }
}