package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.GrammarReferenceMap;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.token.ListTokenStream;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer.AssignedToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RepeatMatcherTest {
	@Test
	void testInstantFail() throws ParsingException {
		var subGrammar = mock(Grammar.class);
		var matcher    = new RepeatMatcher(subGrammar);
		when(subGrammar.parse(any(), any())).thenThrow(new ParsingException("","",0,0,0));
		var result = matcher.parse(new ListTokenStream(List.of()), new GrammarReferenceMap());
		assertEquals(0, result.getList().size());
	}

	@Test
	void testWorking() throws ParsingException {
		var subGrammar   = mock(Grammar.class);
		var matcher      = new RepeatMatcher(subGrammar);
		var tokenResult  = new TokenNode(List.of(new AssignedToken("NUMBER", "1")));
		var tokenResult2 = new TokenNode(List.of(new AssignedToken("LETTERS", "ABC")));
		var i = 0;
		var streamMock = mock(TokenStream.class);
		when(streamMock.index()).thenReturn(1,2,3,4);
		when(subGrammar.parse(any(), any()))
				.thenReturn(tokenResult, tokenResult2)
				.thenThrow(new ParsingException("","", 0, 0, 0));
		var result = matcher.parse(streamMock, new GrammarReferenceMap());
		System.out.println(result);
		assertEquals(2, result.getList().size());
		assertThat(result.getList())
				.containsExactly(tokenResult, tokenResult2);
	}
}