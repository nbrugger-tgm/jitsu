package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarReferenceMap;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.DefaultToken;
import com.niton.parser.token.ListTokenStream;
import com.niton.parser.token.Tokenizer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

class KeywordMatcherTest extends AbstractMatcherTest {

    @Override
    public GrammarReference getGrammarReference() {
        return new GrammarReferenceMap();
    }

    @Override
    public Stream<TestCase> getTestCases() throws ParsingException {
        var tokenizer = new Tokenizer();
        return Stream.of(
                new TestCase(
                        tokenizer.tokenize("if()"),
                        new KeywordMatcher("if"),
                        new TokenNode(List.of(new Tokenizer.AssignedToken("if", DefaultToken.LETTERS.name(), 0)))
                ),
                new TestCase(
                        tokenizer.tokenize("if_then()"),
                        new KeywordMatcher("if_then"),
                        new TokenNode(List.of(
                                new Tokenizer.AssignedToken("if", DefaultToken.LETTERS.name(), 0),
                                new Tokenizer.AssignedToken("_", DefaultToken.UNDERSCORE.name(), 2),
                                new Tokenizer.AssignedToken("then", DefaultToken.LETTERS.name(), 3)
                        ))
                )
        );
    }

    @Test
    void failOnSingleToken() {
        var tokenizer = new Tokenizer();
        var matcher = new KeywordMatcher("while");
        Assertions.assertThatThrownBy(
                () -> matcher.process(new ListTokenStream(tokenizer.tokenize("if(true)")), new GrammarReferenceMap())
        ).hasMessageContaining("while").hasMessageContaining("if");
    }

    @Test
    void failOnMultipleTokens() {
        var tokenizer = new Tokenizer();
        var matcher = new KeywordMatcher("while_true");
        Assertions.assertThatThrownBy(
                () -> matcher.process(new ListTokenStream(tokenizer.tokenize("while(true)")), new GrammarReferenceMap())
        ).hasMessageContaining("while_true", "while(");
    }

    @Test
    void failOnMultipleTokensEarly() {
        var tokenizer = new Tokenizer();
        var matcher = new KeywordMatcher("while_true");
        Assertions.assertThatThrownBy(
                () -> matcher.process(new ListTokenStream(tokenizer.tokenize("if(true)")), new GrammarReferenceMap())
        ).hasMessageContaining("while_true", "if");
    }
}