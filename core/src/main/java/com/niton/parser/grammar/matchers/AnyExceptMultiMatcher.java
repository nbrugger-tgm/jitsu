package com.niton.parser.grammar.matchers;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.ast.TokenNode;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer.AssignedToken;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * This grammar accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fullfilles
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class AnyExceptMultiMatcher extends GrammarMatcher<TokenNode> {

	private Grammar<?,?>[] dunnoaccept;

	public AnyExceptMultiMatcher(Grammar<?,?>[] string, String origin) {
		this.dunnoaccept = string;
		setOriginGrammarName(origin);
	}

	@Override
	public @NotNull TokenNode process(@NotNull TokenStream tokens, @NotNull GrammarReference reference)
	throws ParsingException {
		TokenNode obj = new TokenNode();

		while (!anyMatch(tokens, reference) && tokens.hasNext()) {
			AssignedToken token = tokens.next();
			obj.tokens.add(token);
		}
		return obj;
	}

	private boolean anyMatch(TokenStream tokens, GrammarReference reference) {
		boolean any = false;
		for (Grammar<?,?> g : dunnoaccept) {
			any |= g.parsable(tokens, reference);
		}
		return any;
	}
}
