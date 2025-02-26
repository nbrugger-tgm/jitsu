package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.GrammarReferenceMap;
import com.niton.jainparse.grammar.types.TokenGrammar;
import com.niton.jainparse.token.DefaultToken;
import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.Tokenizer;
import com.niton.jainparse.token.Tokenizer.AssignedToken;

import java.util.List;
import java.util.stream.Stream;

public class TokenGrammarMatcherTest extends AbstractMatcherTest {
    @Override
    public GrammarReference getGrammarReference() {
        return new GrammarReferenceMap();
    }

    @Override
    protected Stream<TestCase> getTestCases()  {
        var number = new TokenMatcher(new TokenGrammar(DefaultToken.NUMBER.name()));
        var letters = new TokenMatcher(new TokenGrammar(DefaultToken.LETTERS.name()));
        var minus = new TokenMatcher(new TokenGrammar(DefaultToken.MINUS.name()));
        var dot = new TokenMatcher(new TokenGrammar(DefaultToken.DOT.name()));
        var tokenizer = new Tokenizer();

        return Stream.of(
                new TestCase(
                        tokenizer.tokenize("1"),
                        number,
                        new TokenNode(
                                List.of(new AssignedToken("1", number.getGrammar(), 0)),
                                Location.of(0, 0, 0, 1)
                        )
                ),
                new TestCase(
                        tokenizer.tokenize("123"),
                        number,
                        new TokenNode(List.of(new AssignedToken("123", number.getGrammar(), 0)),
                                Location.of(0, 0, 0, 3)
                        )
                ),
                new TestCase(
                        tokenizer.tokenize("123.456"),
                        number,
                        new TokenNode(List.of(new AssignedToken("123", number.getGrammar(), 0)),
                                Location.of(0, 0, 0, 3)
                        )
                ),
                new TestCase(
                        tokenizer.tokenize("1.456"),
                        number,
                        new TokenNode(List.of(new AssignedToken("1", number.getGrammar(), 0)),
                                Location.of(0, 0, 0, 1)
                        )
                ),
                new TestCase(
                        tokenizer.tokenize(".456"),
                        number,
                        null
                ),
                new TestCase(
                        tokenizer.tokenize(".456"),
                        dot,
                        new TokenNode(List.of(new AssignedToken(".", dot.getGrammar(), 0)),
                                Location.of(0, 0, 0, 1)
                        )
                ),
                new TestCase(
                        tokenizer.tokenize("..456"),
                        dot,
                        new TokenNode(List.of(new AssignedToken(".", dot.getGrammar(), 0)),
                                Location.of(0, 0, 0, 1)
                        )
                ),
                new TestCase(
                        tokenizer.tokenize(""),
                        dot,
                        null
                ),
                new TestCase(
                        tokenizer.tokenize("AABBCC"),
                        letters,
                        new TokenNode(
                                List.of(new AssignedToken("AABBCC", letters.getGrammar(), 0)),
                                Location.of(0, 0, 0, 6)
                        )
                ),
                new TestCase(
                        tokenizer.tokenize("AaBbCc"),
                        letters,
                        new TokenNode(List.of(new AssignedToken("AaBbCc", letters.getGrammar(), 0)),
                                Location.of(0, 0, 0, 6)
                        )
                ),
                new TestCase(
                        tokenizer.tokenize(""),
                        letters,
                        null
                ),
                new TestCase(
                        tokenizer.tokenize("123-45"),
                        minus,
                        new TokenNode(List.of(new AssignedToken("-", minus.getGrammar(), 3)),
                                Location.of(0, 0, 3, 4)
                        ),
                        1
                )
        );
    }
}
