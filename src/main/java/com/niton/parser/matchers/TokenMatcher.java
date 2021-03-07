package com.niton.parser.matchers;

import com.niton.parser.Grammar;
import com.niton.parser.GrammarMatcher;
import com.niton.parser.token.TokenStream;
import com.niton.parser.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.result.TokenGrammarResult;
import com.niton.parser.token.Tokenizer.AssignedToken;

/**
 * Simply matches a token
 * 
 * @author Nils
 * @version 2019-05-28
 */
public class TokenMatcher extends GrammarMatcher<TokenGrammarResult> {
	private String tokenName;

	/**
	 * Creates an Instance of TokenGrammar.java
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 * @param tokenName
	 */
	public TokenMatcher(String tokenName) {
		this.tokenName = tokenName;
	}

	/**
	 * @throws ParsingException
	 * @see Grammar#process(java.util.List,GrammarReference)
	 */
	@Override
	public TokenGrammarResult process(TokenStream tokens, GrammarReference ref) throws ParsingException {
		AssignedToken token = tokens.next();
		if (token.name.equals(tokenName)) {
			TokenGrammarResult obj = new TokenGrammarResult();
			obj.tokens.add(token);
			return obj;
		}
		throw new ParsingException("Expected Token \"" + tokenName + "\" but actual value was  \"" + token.name+"\" (index : "+tokens.index()+") -> "+"["+token.value+"]");
	}

	/**
	 * @return the tokenName
	 */
	public String getTokenName() {
		return tokenName;
	}

	/**
	 * @param tokenName the tokenName to set
	 */
	public void setTokenName(String tokenName) {
		this.tokenName = tokenName;
	}
}
