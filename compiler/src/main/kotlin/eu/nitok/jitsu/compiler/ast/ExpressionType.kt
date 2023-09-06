package eu.nitok.jitsu.compiler.ast

import com.niton.parser.grammar.api.GrammarName

enum class ExpressionType : GrammarName {
    LITERAL_EXPRESSION,
    STATEMENT_EXPRESSION,
    ENCLOSED_EXPRESSION,
    OPERATION_EXPRESSION,
    FIELD_ACCESS_EXPRESSION,
    METHOD_INVOCATION,
    INDEXED_ACCESS_EXPRESSION;

    override fun getName(): String {
        return this.name
    }
}