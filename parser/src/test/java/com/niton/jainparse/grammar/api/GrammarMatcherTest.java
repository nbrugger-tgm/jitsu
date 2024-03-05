package com.niton.jainparse.grammar.api;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.token.ListTokenStream;
import com.niton.jainparse.token.TokenStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.*;

class GrammarMatcherTest {

	private GrammarMatcher<?> matcher;

	@BeforeEach
	void setUp() throws Exception {
		matcher = new GrammarMatcher<OptionalNode>() {
			@Override
			protected @NotNull ParsingResult<OptionalNode> process(
					@NotNull TokenStream tokens, @NotNull GrammarReference reference
			) {
				return ParsingResult.ok(mock(OptionalNode.class));
			}
		};
		matcher.setOriginGrammarName("test_grammar");
	}

	@Test
	void parseElevatesStack() {
		var ref = mock(GrammarReference.class);
		var tokens = spy(new ListTokenStream(List.of()));

		matcher.parse(tokens, ref);

		verify(tokens).elevate();
	}

	@Test
	void parseCommitsStack() {
		var ref = mock(GrammarReference.class);
		var tokens = spy(new ListTokenStream(List.of()));

		matcher.parse(tokens, ref);

		verify(tokens).commit();
	}

	@Test
	void parseNamesResult()  {
		var ref = mock(GrammarReference.class);
		var tokens = spy(new ListTokenStream(List.of()));

		var result = matcher.parse(tokens, ref);

		assertThat(result.unwrap().getOriginGrammarName()).isEqualTo("test_grammar");
	}

	@Test
	void parseRevertOnError() {
		matcher = new GrammarMatcher<>() {
			@Override
			protected @NotNull ParsingResult<AstNode> process(
					@NotNull TokenStream tokens, @NotNull GrammarReference reference
			)  {
				return ParsingResult.error(new ParsingException("test","",tokens.currentLocation()));
			}
		};
		var ref = mock(GrammarReference.class);
		var tokens = spy(new ListTokenStream(List.of()));

		assertThatCode(()->matcher.parse(tokens, ref)).isInstanceOf(ParsingException.class);
		verify(tokens).rollback();
	}

	@Test
	void onElevateFail(){
		var ref = mock(GrammarReference.class);
		var tokens = spy(new ListTokenStream(List.of()));
		doThrow(new IllegalStateException("test")).when(tokens).elevate();
		assertThatCode(()->matcher.parse(tokens, ref))
				.isInstanceOf(ParsingException.class)
				.hasMessageContaining("test_grammar");
	}

	@Test
	void remainOnRoot(){
		var ref = mock(GrammarReference.class);
		var tokens = spy(new ListTokenStream(List.of()));
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
		var tokens = spy(new ListTokenStream(List.of()));
		when(tokens.level()).thenReturn(0);
		when(tokens.size()).thenReturn(10);
		when(tokens.index()).thenReturn(10);
		assertThatCode(()->matcher.parse(tokens, ref))
				.doesNotThrowAnyException();
	}
}