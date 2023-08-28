package eu.nitok.jitsu.compiler.parser

import com.niton.parser.exceptions.ParsingException
import com.niton.parser.grammar.api.GrammarReferenceMap
import com.niton.parser.token.ListTokenStream
import com.niton.parser.token.Tokenizer
import org.junit.jupiter.api.Test

class SyntaxTests {
    @Test
    fun methodInvocation(){
        val tokenizer = Tokenizer()
        tokenizer.isIgnoreEOF = true;
        val txt = ListTokenStream(tokenizer.tokenize("a.b(xaxa).c()"));
        val grammar = methodInvocation;
        try {
            val process = grammar.parse(txt, grammarReference)
            println(process.reduce("invocation").orElseThrow().format())
        }catch (e:ParsingException){
            println(e.mostProminentDeepException.markInText(txt.toString(), 2));
            println(e.fullExceptionTree)
            throw RuntimeException("failed to parse")
        }
    }
}