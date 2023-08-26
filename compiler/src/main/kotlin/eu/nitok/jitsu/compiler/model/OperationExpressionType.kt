package eu.nitok.jitsu.compiler.model

import com.niton.parser.grammar.api.GrammarName

enum class OperationExpressionType : GrammarName{
    ADD_EXPRESSION,
    MULTIPLY_EXPRESSION,
    DIVIDE_EXPRESSION,
    SUBTRACT_EXPRESSION;

    override fun getName(): String {
        return name
    }
}