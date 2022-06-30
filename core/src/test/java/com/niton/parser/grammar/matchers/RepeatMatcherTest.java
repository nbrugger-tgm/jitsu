package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.GrammarReferenceMap;
import com.niton.parser.grammar.api.Grammar;
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
		when(subGrammar.parse(any(), any())).thenThrow(new ParsingException(""));
		var result = matcher.parse(new TokenStream(List.of()), new GrammarReferenceMap());
		assertEquals(0, result.getList().size());
	}

	@Test
	void testWorking() throws ParsingException {
		var subGrammar   = mock(Grammar.class);
		var matcher      = new RepeatMatcher(subGrammar);
		var tokenResult  = new TokenNode(List.of(new AssignedToken("NUMBER", "1")));
		var tokenResult2 = new TokenNode(List.of(new AssignedToken("LETTERS", "ABC")));
		when(subGrammar.parse(any(), any()))
				.thenReturn(tokenResult, tokenResult2)
				.thenThrow(new ParsingException(""));
		var result = matcher.parse(new TokenStream(List.of()), new GrammarReferenceMap());
		assertEquals(2, result.getList().size());
		assertThat(result.getList())
				.containsExactly(tokenResult, tokenResult2);
	}
}