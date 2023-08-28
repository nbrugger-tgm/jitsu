package eu.nitok.jitsu.compiler.model

import com.niton.parser.grammar.api.GrammarName

enum class ExpressionType : GrammarName {
    LITERAL_EXPRESSION,
    STATEMENT_EXPRESSION,
    ENCLOSED_EXPRESSION,
    OPERATION_EXPRESSION;

    override fun getName(): String {
        return this.name
    }
}