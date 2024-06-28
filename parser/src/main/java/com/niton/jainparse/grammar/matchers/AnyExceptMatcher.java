package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenable;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * This grammar accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fullfilles
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class AnyExceptMatcher<T extends Enum<T> & Tokenable> extends GrammarMatcher<TokenNode<T>,T> {

	private Grammar<?,T> dunnoaccept;

	public AnyExceptMatcher(Grammar<?,T> grammar) {
		this.dunnoaccept = grammar;
	}

	/**
	 * @see GrammarMatcher#process(TokenStream, GrammarReference)
	 * @param tokens
	 * @param reference
	 */
	@Override
	public @NotNull ParsingResult<TokenNode<T>> process(@NotNull TokenStream<T> tokens, @NotNull GrammarReference<T> reference) {
		List<AssignedToken<T>> matchedTokens = new LinkedList<>();
		int startLine = tokens.getLine();
		int startColumn = tokens.getColumn();
		while (!dunnoaccept.parsable(tokens, reference) && tokens.hasNext()) {
			var token = tokens.next();
			matchedTokens.add(token);
		}
		int endLine = tokens.getLine();
		int endColumn = tokens.getColumn();
		return ParsingResult.ok(new TokenNode<>(matchedTokens, Location.of(startLine, startColumn, endLine, endColumn)));
	}

}
