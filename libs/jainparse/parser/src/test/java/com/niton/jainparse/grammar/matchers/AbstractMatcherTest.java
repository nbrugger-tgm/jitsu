package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.token.ListTokenStream;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenizer;
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
	Stream<DynamicTest> parsingTest()  {
		final var ref = getGrammarReference();
		return getTestCases().map(testCase -> DynamicTest.dynamicTest(
				"Test: " + testCase.inputTokens.stream().map(Tokenizer.AssignedToken::getValue).collect(Collectors.joining()),
				() -> {
					TokenStream tokenStream = new ListTokenStream(testCase.inputTokens);
					for (int i = 0; i < testCase.parseFrom; i++) tokenStream.next();
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

	protected abstract GrammarReference getGrammarReference() ;

	protected abstract Stream<TestCase> getTestCases() ;
}