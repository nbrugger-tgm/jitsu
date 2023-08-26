package eu.nitok.jitsu.compiler.parser

import com.niton.parser.grammar.api.GrammarReferenceMap
import com.niton.parser.token.ListTokenStream
import com.niton.parser.token.Tokenizer
import org.junit.jupiter.api.Test

class SyntaxTests {
    @Test
    fun methodInvocation(){
        val tokenizer = Tokenizer()
        tokenizer.isIgnoreEOF = true;
        var txt = ListTokenStream(tokenizer.tokenize("a.b(xaxa).c()"));
        val grammar = methodInvocation;
        val process = grammar.parse(txt, GrammarReferenceMap())
        println(process.reduce("invocation").orElseThrow().format())
    }
}