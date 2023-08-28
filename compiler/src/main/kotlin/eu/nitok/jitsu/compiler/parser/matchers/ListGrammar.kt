package eu.nitok.jitsu.compiler.parser.matchers

import com.niton.parser.ast.SequenceNode
import com.niton.parser.grammar.api.Grammar
import com.niton.parser.grammar.api.GrammarMatcher

class ListGrammar(
    val element: Grammar<*>,
    val separator: Grammar<*>,
    private var minSize: Int? = 0,
) : Grammar<SequenceNode>() {
    override fun copy(): Grammar<*> {
        return ListGrammar(element, separator)
    }

    override fun createExecutor(): GrammarMatcher<SequenceNode> {
        return ListMatcher(this,minSize)
    }

    fun setMinSize(i: Int): ListGrammar {
        this.minSize = i;
        return this
    }
}