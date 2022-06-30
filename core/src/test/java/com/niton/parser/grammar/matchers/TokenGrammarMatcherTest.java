package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.GrammarReferenceMap;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.TokenGrammar;
import com.niton.parser.token.DefaultToken;
import com.niton.parser.token.Tokenizer;
import com.niton.parser.token.Tokenizer.AssignedToken;

import java.util.List;
import java.util.stream.Stream;

public class TokenGrammarMatcherTest extends AbstractMatcherTest{
	@Override
	public GrammarReference getGrammarReference() {
		return new GrammarReferenceMap();
	}

	@Override
	public Stream<TestCase> getTestCases() throws ParsingException {
		var number = new TokenMatcher(new TokenGrammar(DefaultToken.NUMBER.name()));
		var letters = new TokenMatcher(new TokenGrammar(DefaultToken.LETTERS.name()));
		var minus = new TokenMatcher(new TokenGrammar(DefaultToken.MINUS.name()));
		var dot = new TokenMatcher(new TokenGrammar(DefaultToken.POINT.name()));
		var tokenizer = new Tokenizer();

		return Stream.of(
			new TestCase(
				tokenizer.tokenize("1"),
				number,
				new TokenNode(List.of(new AssignedToken("1",number.getTokenName(),0)))
			),
			new TestCase(
					tokenizer.tokenize("123"),
					number,
					new TokenNode(List.of(new AssignedToken("123",number.getTokenName(),0)))
			),
			new TestCase(
					tokenizer.tokenize("123.456"),
					number,
					new TokenNode(List.of(new AssignedToken("123",number.getTokenName(),0)))
			),
			new TestCase(
					tokenizer.tokenize("1.456"),
					number,
					new TokenNode(List.of(new AssignedToken("1",number.getTokenName(),0)))
			),
			new TestCase(
					tokenizer.tokenize(".456"),
					number,
					null
			),
			new TestCase(
					tokenizer.tokenize(".456"),
					dot,
					new TokenNode(List.of(new AssignedToken(".",dot.getTokenName(),0)))
			),
			new TestCase(
					tokenizer.tokenize("..456"),
					dot,
					new TokenNode(List.of(new AssignedToken(".",dot.getTokenName(),0)))
			),
			new TestCase(
					tokenizer.tokenize(""),
					dot,
					null
			),
			new TestCase(
					tokenizer.tokenize("AABBCC"),
					letters,
					new TokenNode(List.of(new AssignedToken("AABBCC",letters.getTokenName(),0)))
			),
			new TestCase(
					tokenizer.tokenize("AaBbCc"),
					letters,
					new TokenNode(List.of(new AssignedToken("AaBbCc",letters.getTokenName(),0)))
			),
			new TestCase(
					tokenizer.tokenize(""),
					letters,
					null
			),
			new TestCase(
					tokenizer.tokenize("123-45"),
					minus,
					new TokenNode(List.of(new AssignedToken("-",minus.getTokenName(),3))),
					1
			)
		);
	}
}
