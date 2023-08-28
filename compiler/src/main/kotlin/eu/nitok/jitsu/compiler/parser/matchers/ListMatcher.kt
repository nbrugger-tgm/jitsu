package eu.nitok.jitsu.compiler.parser.matchers

import com.niton.parser.ast.AstNode
import com.niton.parser.ast.SequenceNode
import com.niton.parser.exceptions.ParsingException
import com.niton.parser.grammar.api.GrammarMatcher
import com.niton.parser.grammar.api.GrammarReference
import com.niton.parser.token.Location
import com.niton.parser.token.TokenStream

class ListMatcher(val grammar: ListGrammar, val minSize: Int?) : GrammarMatcher<SequenceNode>() {

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
                    exceptions.add(e);
                    throw ParsingException(originGrammarName,"Expected element after separator", exceptions.toTypedArray())
                }
                if(element.parsingException != null) exceptions.add(element.parsingException);
                list.add(element)
            }
        } catch (e: ParsingException) {
            exceptions.add(e)
        }
        val node : SequenceNode;
        if(list.isEmpty()) node = SequenceNode(Location.oneChar(tokens.line, tokens.column));
        else node = SequenceNode(list, (0..<list.size).groupBy { it.toString() }.mapValues { it.value[0] });
        if(minSize != null && list.size < minSize) {
            throw ParsingException(originGrammarName,"List size is smaller than $minSize", exceptions.toTypedArray())
        }
        node.parsingException = if(exceptions.isEmpty()) null else ParsingException(identifier, "List parsing succeeded with allowed exceptions", exceptions.toTypedArray())
        return node;
    }
}