package com.niton.parser.matchers;

import com.niton.parser.Grammar;
import com.niton.parser.GrammarMatcher;
import com.niton.parser.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammars.TokenGrammar;
import com.niton.parser.result.TokenGrammarResult;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer.AssignedToken;

/**
 * Simply matches a token
 *
 * @author Nils
 * @version 2019-05-28
 */
public class TokenMatcher extends GrammarMatcher<TokenGrammarResult> {
	private TokenGrammar     tokenName;
	private ParsingException interrupt;

	/**
	 * Creates an Instance of TokenGrammar.java
	 *
	 * @param tokenName
	 * @author Nils
	 * @version 2019-05-28
	 */
	public TokenMatcher(TokenGrammar tokenName) {
		this.tokenName = tokenName;
	}

	@Override
	public ParsingException getLastException() {
		return interrupt;
	}

	/**
	 * @throws ParsingException
	 * @see Grammar#process(java.util.List, GrammarReference)
	 */
	@Override
	public TokenGrammarResult process(TokenStream tokens, GrammarReference ref)
	throws ParsingException {
		AssignedToken token = tokens.next();
		if (token.name.equals(tokenName.getTokenName())) {
			TokenGrammarResult obj = new TokenGrammarResult();
			obj.tokens.add(token);
			return obj;
		}
		throw interrupt = new ParsingException(this.tokenName + " Expected Token \"" + tokenName.getTokenName() + "\" but actual value was  \"" + token.name + "\" (index : " + tokens
				.index() + ") -> " + "[" + token.value + "]");
	}

	/**
	 * @return the tokenName
	 */
	public String getTokenName() {
		return tokenName.getTokenName();
	}

	/**
	 * @param tokenName the tokenName to set
	 */
	public void setTokenName(TokenGrammar tokenName) {
		this.tokenName = tokenName;
	}
}
