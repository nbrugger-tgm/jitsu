package eu.nitok.jitsu.compiler.model

import com.niton.parser.grammar.api.GrammarName

enum class StatementType : GrammarName {
    VARIABLE_DECLARATION,
    FUNCTION_DECLARATION,
    FUNCTION_CALL,
    METHOD_INVOCATION,
    STATEMENT_WITH_SEMICOLON,
    RETURN_STATEMENT,
    ASSIGNMENT,
    IF_STATEMENT,
    CODE_BLOCK,
    SWITCH_STATEMENT,
    YIELD_STATEMENT;

    override fun getName(): String {
        return this.name
    }
}