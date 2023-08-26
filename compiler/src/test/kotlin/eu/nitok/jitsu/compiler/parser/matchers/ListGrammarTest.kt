package eu.nitok.jitsu.compiler.parser.matchers

import com.niton.parser.grammar.api.Grammar.token
import com.niton.parser.grammar.api.GrammarReferenceMap
import com.niton.parser.token.DefaultToken
import com.niton.parser.token.ListTokenStream
import com.niton.parser.token.Tokenizer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ListGrammarTest {
    @Test
    fun testProcess() {
        val tokenizer = Tokenizer()
        tokenizer.isIgnoreEOF = true;
        var txt = ListTokenStream(tokenizer.tokenize("a,b"));
        val grammar = ListGrammar(token(DefaultToken.LETTERS), token(DefaultToken.COMMA));
        val process = grammar.parse(txt, GrammarReferenceMap());
        assertEquals(2, process.subNodes.size);
        assertEquals("a", process.subNodes[0].joinTokens());
        assertEquals("b", process.subNodes[1].joinTokens());
    }
}