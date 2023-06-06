package com.niton.parser;

import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.ListTokenStream;
import com.niton.parser.token.TokenSource;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParserTest {

	private static final String ORIG_GRAMMAR_NAME = "test_grammar";
	@Mock
	AstNode             node;
	@Mock
	Grammar<AstNode> grammar;
	Parser<String> parser;
	@Mock
	Tokenizer tokenizer;
	@Test
	void parseNull() {
		mockParser();
		assertThatCode(() -> parser.parse((String) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);

		assertThatCode(() -> parser.parse((InputStream) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);

		assertThatCode(() -> parser.parse((TokenSource) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);
		assertThatCode(() -> parser.parse((TokenStream) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);

		assertThatCode(() -> parser.parse((Reader) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);

		assertThatCode(() -> parser.parse((List<Tokenizer.AssignedToken>) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void parsePlainNull() {
		mockParser();
		assertThatCode(() -> parser.parsePlain((String) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);

		assertThatCode(() -> parser.parsePlain((InputStream) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);

		assertThatCode(() -> parser.parsePlain((TokenSource) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);
		assertThatCode(() -> parser.parsePlain((TokenStream) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);

		assertThatCode(() -> parser.parsePlain((Reader) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);

		assertThatCode(() -> parser.parsePlain((List<Tokenizer.AssignedToken>) null))
				.as("Trying to parse null should throw an exception")
				.isInstanceOf(NullPointerException.class);
	}

	void mockParser() {
		parser = new Parser<>(grammar) {
			@Override
			@NotNull
			public String convert(@NonNull AstNode o) {
				return o.getOriginGrammarName();
			}
		};
		parser.setTokenizer(tokenizer);
	}

	@Test
	void parsePlainReturn() throws ParsingException {
		mockGrammar();
		assertThat(parser.parsePlain(""))
				.as("should return the grammar parsing result")
				.isNotNull()
				.extracting(AstNode::getOriginGrammarName)
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parsePlain(mock(InputStream.class)))
				.isNotNull()
				.as("should return the grammar parsing result")
				.extracting(AstNode::getOriginGrammarName)
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parsePlain(mock(ListTokenStream.class)))
				.isNotNull()
				.as("should return the grammar parsing result")
				.extracting(AstNode::getOriginGrammarName)
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parsePlain(mock(TokenSource.class)))
				.isNotNull()
				.as("should return the grammar parsing result")
				.extracting(AstNode::getOriginGrammarName)
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parsePlain(new StringReader("")))
				.isNotNull()
				.as("should return the grammar parsing result")
				.extracting(AstNode::getOriginGrammarName)
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parsePlain(new LinkedList<>()))
				.isNotNull()
				.as("should return the grammar parsing result")
				.extracting(AstNode::getOriginGrammarName)
				.isEqualTo(ORIG_GRAMMAR_NAME);
	}
	@Test
	void parseReturn() throws ParsingException {
		mockGrammar();
		assertThat(parser.parse(""))
				.isNotNull()
				.as("should return the grammar parsing result")
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parse(mock(InputStream.class)))
				.isNotNull()
				.as("should return the grammar parsing result")
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parse(mock(ListTokenStream.class)))
				.isNotNull()
				.as("should return the grammar parsing result")
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parse(mock(TokenSource.class)))
				.isNotNull()
				.as("should return the grammar parsing result")
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parse(new StringReader("")))
				.isNotNull()
				.as("should return the grammar parsing result")
				.isEqualTo(ORIG_GRAMMAR_NAME);

		assertThat(parser.parse(new LinkedList<>()))
				.isNotNull()
				.as("should return the grammar parsing result")
				.isEqualTo(ORIG_GRAMMAR_NAME);
	}

	void mockGrammar() throws ParsingException {
		when(node.getOriginGrammarName()).thenReturn(ORIG_GRAMMAR_NAME);
		when(grammar.parse(any(), any())).thenReturn(node);
		mockParser();
	}

	@Test
	void setTokenizer() throws ParsingException {
		mockGrammar();
		var newTokenizer = mock(Tokenizer.class);

		parser.setTokenizer(newTokenizer);

		parser.parse("");

		verify(newTokenizer).tokenize(any());
		verifyNoInteractions(tokenizer);
	}

	@Test
	void setReference() throws ParsingException {
		mockParser();

		var mockedRoot = mock(Grammar.class);
		var newReference = mock(GrammarReference.class);
		when(newReference.get("test-root")).thenReturn(mockedRoot);

		parser.setReference(newReference);
		parser.setRoot("test-root");
		parser.parsePlain("");

		verify(newReference).get("test-root");
		verify(mockedRoot).parse(any(), any());
	}
}