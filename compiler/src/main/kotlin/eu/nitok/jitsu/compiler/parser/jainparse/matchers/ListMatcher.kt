package eu.nitok.jitsu.compiler.parser.jainparse.matchers

import com.niton.parser.ast.AstNode
import com.niton.parser.ast.ParsingResult
import com.niton.parser.ast.SequenceNode
import com.niton.parser.exceptions.ParsingException
import com.niton.parser.grammar.api.GrammarMatcher
import com.niton.parser.grammar.api.GrammarReference
import com.niton.parser.token.Location
import com.niton.parser.token.TokenStream

class ListMatcher(val grammar: ListGrammar, val minSize: Int?) : GrammarMatcher<SequenceNode>() {

    @OptIn(ExperimentalStdlibApi::class)
    override fun process(tokens: TokenStream, reference: GrammarReference): ParsingResult<SequenceNode> {
        val exceptions = mutableListOf<ParsingException>()
        val firstElementResult = grammar.element.parse(tokens, reference)
        val deepResult = firstElementResult.map { firstElement ->
            val list = mutableListOf<AstNode>()
            if (firstElement.parsingException != null) exceptions.add(firstElement.parsingException);
            list.add(firstElement)
            while (true) {
                val separator = grammar.separator.parse(tokens, reference)
                if (!separator.wasParsed()) {
                    exceptions.add(separator.exception())
                    break
                }
                val sepearatorException = separator.unwrap().parsingException
                if (sepearatorException != null) exceptions.add(sepearatorException);
                val elementResult = grammar.element.parse(tokens, reference);
                if (!elementResult.wasParsed()) {
                    exceptions.add(elementResult.exception());
                    return@map ParsingResult.error(
                        ParsingException(
                            originGrammarName,
                            "Expected element after separator",
                            exceptions.toTypedArray()
                        )
                    )
                }
                val element = elementResult.unwrap()
                if (element.parsingException != null) exceptions.add(element.parsingException);
                list.add(element)
            }
            return@map ParsingResult.ok(list);
        };
        val list = if (!deepResult.wasParsed()) {
            exceptions.add(deepResult.exception()); emptyList()
        } else {
            val listResult = deepResult.unwrap();
            if (!listResult.wasParsed())
                return ParsingResult.error(listResult.exception())
            listResult.unwrap()
        };
        val node: SequenceNode;
        if (list.isEmpty()) node = SequenceNode(Location.oneChar(tokens.line, tokens.column));
        else node = SequenceNode(list, (0..<list.size).groupBy { it.toString() }.mapValues { it.value[0] });
        if (minSize != null && list.size < minSize) {
            return ParsingResult.error(
                ParsingException(
                    originGrammarName,
                    "List size is smaller than $minSize",
                    exceptions.toTypedArray()
                )
            )
        }
        node.parsingException = if (exceptions.isEmpty()) null else ParsingException(
            identifier,
            "List parsing succeeded with allowed exceptions",
            exceptions.toTypedArray()
        )
        return ParsingResult.ok(node);
    }
}