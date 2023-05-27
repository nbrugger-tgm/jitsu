package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.GrammarReferenceMap;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReferenceGrammarMatcherTest extends AbstractMatcherTest {

	public AstNode newNode = new TokenNode(List.of());
	@Override
	public GrammarReference getGrammarReference() throws ParsingException {
		var mocked1 = mock(Grammar.class);
		var mocked3 = mock(Grammar.class);
		when(mocked3.parse(any(),any())).thenThrow(new ParsingException("","",0,0,0));
		when(mocked1.parse(any(),any())).thenReturn(newNode);
		GrammarReferenceMap map = new GrammarReferenceMap();
		map.map(mocked1, "test");
		map.map(mocked3, "test2");
		return map;
	}

	@Override
	public Stream<TestCase> getTestCases() {
		return Stream.of(
				new TestCase(List.of(),new ReferenceGrammarMatcher("test"), newNode),
				new TestCase(List.of(),new ReferenceGrammarMatcher("test2"), null),
				new TestCase(List.of(),new ReferenceGrammarMatcher("test3"), null)
		);
	}
}