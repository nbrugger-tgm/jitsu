package com.niton.parser.grammar.api;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.IgnoredNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.TokenStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.Mockito.*;

class GrammarMatcherTest {

	private GrammarMatcher matcher;

	@BeforeEach
	void setUp() throws Exception {
		matcher = new GrammarMatcher() {
			@NotNull
			@Override
			protected AstNode process(
					@NotNull TokenStream tokens, @NotNull GrammarReference reference
			) {
				return new IgnoredNode();
			}
		};
		matcher.setOriginGrammarName("test_grammar");
	}

	@Test
	void parseElevatesStack() throws ParsingException {
		var ref = mock(GrammarReference.class);
		var tokens = spy(new TokenStream(List.of()));

		matcher.parse(tokens, ref);

		verify(tokens).elevate();
	}

	@Test
	void parseCommitsStack() throws ParsingException {
		var ref = mock(GrammarReference.class);
		var tokens = spy(new TokenStream(List.of()));

		matcher.parse(tokens, ref);

		verify(tokens).commit();
	}

	@Test
	void parseNamesResult() throws ParsingException {
		var ref = mock(GrammarReference.class);
		var tokens = spy(new TokenStream(List.of()));

		var result = matcher.parse(tokens, ref);

		assertThat(result.getOriginGrammarName()).isEqualTo("test_grammar");
	}

	@Test
	void parseRevertOnError() {
		matcher = new GrammarMatcher() {
			@NotNull
			@Override
			protected AstNode process(
					@NotNull TokenStream tokens, @NotNull GrammarReference reference
			) throws ParsingException {
				throw new ParsingException("test");
			}
		};
		var ref = mock(GrammarReference.class);
		var tokens = spy(new TokenStream(List.of()));

		assertThatCode(()->matcher.parse(tokens, ref)).isInstanceOf(ParsingException.class);
		verify(tokens).rollback();
	}

	@Test
	void onElevateFail(){
		var ref = mock(GrammarReference.class);
		var tokens = spy(new TokenStream(List.of()));
		doThrow(new IllegalStateException("test")).when(tokens).elevate();
		assertThatCode(()->matcher.parse(tokens, ref))
				.isInstanceOf(ParsingException.class)
				.hasMessageContaining("test_grammar");
	}

	@Test
	void remainOnRoot(){
		var ref = mock(GrammarReference.class);
		var tokens = spy(new TokenStream(List.of()));
		when(tokens.level()).thenReturn(0);
		when(tokens.size()).thenReturn(10);
		when(tokens.index()).thenReturn(5);
		assertThatCode(()->matcher.parse(tokens, ref))
				.isInstanceOf(ParsingException.class)
				.hasMessageContaining("tokens consumed");
	}
	@Test
	void remainOnNonRoot(){
		var ref = mock(GrammarReference.class);
		var tokens = spy(new TokenStream(List.of()));
		when(tokens.level()).thenReturn(0);
		when(tokens.size()).thenReturn(10);
		when(tokens.index()).thenReturn(10);
		assertThatCode(()->matcher.parse(tokens, ref))
				.doesNotThrowAnyException();
	}
}