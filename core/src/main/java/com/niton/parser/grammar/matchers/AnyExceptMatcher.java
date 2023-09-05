package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.ParsingResult;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.ast.TokenNode;
import com.niton.parser.token.Location;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer.AssignedToken;
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
public class AnyExceptMatcher extends GrammarMatcher<TokenNode> {

	private Grammar<?> dunnoaccept;

	public AnyExceptMatcher(Grammar<?> grammar) {
		this.dunnoaccept = grammar;
	}

	/**
	 * @see GrammarMatcher#process(TokenStream, GrammarReference)
	 * @param tokens
	 * @param reference
	 */
	@Override
	public @NotNull ParsingResult<TokenNode> process(@NotNull TokenStream tokens, @NotNull GrammarReference reference) {
		List<AssignedToken> matchedTokens = new LinkedList<>();
		int startLine = tokens.getLine();
		int startColumn = tokens.getColumn();
		while (!dunnoaccept.parsable(tokens, reference) && tokens.hasNext()) {
			AssignedToken token = tokens.next();
			matchedTokens.add(token);
		}
		int endLine = tokens.getLine();
		int endColumn = tokens.getColumn();
		return ParsingResult.ok(new TokenNode(matchedTokens, Location.of(startLine, startColumn, endLine, endColumn)));
	}

}
