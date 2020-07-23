package com.niton.parser.grammars;

import com.niton.parser.Grammar;
import com.niton.parser.result.TokenGrammarResult;
import com.niton.parser.matchers.TokenMatcher;

/**
 * This is the TokenGrammar Class
 * 
 * @author Nils
 * @version 2019-05-28
 */
public class TokenGrammar extends Grammar<TokenMatcher,TokenGrammarResult> {
	private String tokenName;

	/**
	 * Creates an Instance of TokenGrammar.java
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 * @param tokenName
	 * @param name
	 */
	public TokenGrammar(String tokenName, String name) {
		super();
		this.tokenName = tokenName;
		setName(name);
	}

	public TokenGrammar(String token) {
		super();
		this.tokenName = token;
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


	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public TokenMatcher createExecutor() {
		return new TokenMatcher(tokenName);
	}

	@Override
	public void reconfigMatcher(TokenMatcher tokenMatcher) {
		tokenMatcher.setTokenName(tokenName);
	}

}
