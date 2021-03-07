package com.niton.parser.matchers;

import com.niton.parser.Grammar;
import com.niton.parser.GrammarMatcher;
import com.niton.parser.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.result.TokenGrammarResult;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer.AssignedToken;

/**
 * This grammar accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fullfilles
 *
 * @author Nils
 * @version 2019-05-29
 */
public class AnyExceptMatcher extends GrammarMatcher<TokenGrammarResult> {

	private Grammar dunnoaccept;

	public AnyExceptMatcher(Grammar grammar) {
		this.dunnoaccept = grammar;
	}

	/**
	 * @return the dunnoaccept
	 */
	public Grammar getDunnoaccept() {
		return dunnoaccept;
	}

	/**
	 * @param dunnoaccept the dunnoaccept to set
	 */
	public void setDunnoaccept(Grammar dunnoaccept) {
		this.dunnoaccept = dunnoaccept;
	}

	@Override
	public ParsingException getLastException() {
		return null;//This grammar cant fail
	}

	/**
	 * @see GrammarMatcher#process(TokenStream, GrammarReference)
	 */
	@Override
	public TokenGrammarResult process(TokenStream tokens, GrammarReference reference)
	throws ParsingException {
		TokenGrammarResult obj = new TokenGrammarResult();
		while (!dunnoaccept.parsable(tokens, reference)) {
			AssignedToken token = tokens.next();
			obj.tokens.add(token);
		}
		return obj;
	}

}
