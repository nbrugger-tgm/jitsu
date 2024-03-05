package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.GrammarReferenceMap;
import com.niton.jainparse.api.Location;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReferenceGrammarMatcherTest extends AbstractMatcherTest {

    public AstNode newNode = new TokenNode(List.of(), Location.of(0, 0, 0, 0));

    @Override
    public GrammarReference getGrammarReference()  {
        var mocked1 = mock(Grammar.class);
        var mocked3 = mock(Grammar.class);
        when(mocked3.parse(any(), any())).thenThrow(new ParsingException("", "", 0, 0, 0));
        when(mocked1.parse(any(), any())).thenReturn(newNode);
        GrammarReferenceMap map = new GrammarReferenceMap();
        map.map(mocked1, "test");
        map.map(mocked3, "test2");
        return map;
    }

    @Override
    public Stream<TestCase> getTestCases() {
        return Stream.of(
                new TestCase(List.of(), new ReferenceGrammarMatcher("test"), newNode),
                new TestCase(List.of(), new ReferenceGrammarMatcher("test2"), null),
                new TestCase(List.of(), new ReferenceGrammarMatcher("test3"), null)
        );
    }
}