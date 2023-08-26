package eu.nitok.jitsu.compiler.parser.matchers

import com.niton.parser.ast.AstNode
import com.niton.parser.ast.SequenceNode
import com.niton.parser.exceptions.ParsingException
import com.niton.parser.grammar.api.GrammarMatcher
import com.niton.parser.grammar.api.GrammarReference
import com.niton.parser.token.TokenStream

class ListMatcher(val grammar: ListGrammar) : GrammarMatcher<SequenceNode>() {

    @OptIn(ExperimentalStdlibApi::class)
    override fun process(tokens: TokenStream, reference: GrammarReference): SequenceNode {
        val list = mutableListOf<AstNode>();
        val exceptions = mutableListOf<ParsingException>()
        try {
            val firstElement = grammar.element.parse(tokens, reference)
            if(firstElement.parsingException != null) exceptions.add(firstElement.parsingException);
            list.add(firstElement)
            while(true) {
                val separator = grammar.separator.parse(tokens, reference)
                if(separator.parsingException != null) exceptions.add(separator.parsingException);
                val element = try { grammar.element.parse(tokens, reference) } catch (e: ParsingException) {
                    throw ParsingException(originGrammarName,"Expected element after separator", e)
                }
                if(element.parsingException != null) exceptions.add(element.parsingException);
                list.add(element)
            }
        } catch (e: ParsingException) {
            exceptions.add(e)
        }
        if(list.isEmpty())
            return SequenceNode(AstNode.Location.oneChar(tokens.line, tokens.column));
        return SequenceNode(list, (0..<list.size).groupBy { it.toString() }.mapValues { it.value[0] })
    }
}