package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

abstract class AbstractMatcherTest {

	@AllArgsConstructor
	@With
	@RequiredArgsConstructor
	protected static class TestCase {
		@NonNull
		private final List<Tokenizer.AssignedToken> inputTokens;
		@NonNull
		private final GrammarMatcher<?>             matcher;
		private final AstNode                       expectedResult;
		private int parseFrom;
	}

	@TestFactory
	Stream<DynamicTest> parsingTest() throws ParsingException {
		final var ref = getGrammarReference();
		return getTestCases().map(testCase -> DynamicTest.dynamicTest(
				"Test: " + testCase.inputTokens.stream().map(Tokenizer.AssignedToken::getValue).collect(Collectors.joining()),
				() -> {
					TokenStream tokenStream = new TokenStream(testCase.inputTokens);
					tokenStream.index(testCase.parseFrom);
					tokenStream.elevate();//To prevent "unprocessed tokens" error
					if (testCase.expectedResult == null) {
						assertThatCode(() -> testCase.matcher.parse(tokenStream, ref))
								.isInstanceOf(ParsingException.class);
					} else {
						AstNode result = testCase.matcher.parse(tokenStream,ref);
						assertThat(result)
								.usingRecursiveComparison()
								.isEqualTo(testCase.expectedResult);
					}
				}
		));
	}

	public abstract GrammarReference getGrammarReference() throws ParsingException;

	public abstract Stream<TestCase> getTestCases() throws ParsingException;
}