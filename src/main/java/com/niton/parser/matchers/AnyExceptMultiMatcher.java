package com.niton.parser.matchers;

import com.niton.parser.GrammarMatcher;
import com.niton.parser.token.TokenStream;
import com.niton.parser.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Tokenizer.AssignedToken;
import com.niton.parser.Grammar;
import com.niton.parser.result.TokenGrammarResult;

/**
 * This grammar accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fullfilles
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class AnyExceptMultiMatcher extends GrammarMatcher<TokenGrammarResult> {
	
	/**
	 * @return the dunnoaccept
	 */
	public Grammar[] getDunnoaccept() {
		return dunnoaccept;
	}

	/**
	 * @param dunnoaccept the dunnoaccept to set
	 */
	public void setDunnoaccept(Grammar[] dunnoaccept) {
		this.dunnoaccept = dunnoaccept;
	}

	private Grammar[] dunnoaccept;

	public AnyExceptMultiMatcher(Grammar[] string,String origin) {
		this.dunnoaccept = string;setOriginGrammarName(origin);
	}

	@Override
	public TokenGrammarResult process(TokenStream tokens, GrammarReference reference) throws ParsingException {
		TokenGrammarResult obj = new TokenGrammarResult();

		while (!anyMatch(tokens,reference)) {
			AssignedToken token = tokens.next();
			obj.tokens.add(token);
		}
		return obj;
	}

	private boolean anyMatch(TokenStream tokens, GrammarReference reference) {
		boolean any = false;
		for (Grammar g : dunnoaccept)
			any |= g.parsable(tokens, reference);
		return any;
	}
}
