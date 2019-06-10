package com.niton.parser.grammar;

import com.niton.parser.GrammarObject;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.grammar.exectors.GrammarExecutor;
import com.niton.parser.grammar.exectors.TokenExecutor;

/**
 * This is the TokenGrammer Class
 * 
 * @author Nils
 * @version 2019-05-28
 */
public class TokenGrammer extends Grammar {
	private String tokenName;

	/**
	 * Creates an Instance of TokenGrammer.java
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 * @param tokenName
	 * @param name
	 */
	public TokenGrammer(String tokenName, String name) {
		super();
		this.tokenName = tokenName;
		setName(name);
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
	 * @see com.niton.parser.grammar.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return TokenGrammarObject.class;
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#getExecutor()
	 */
	@Override
	public GrammarExecutor getExecutor() {
		return new TokenExecutor(tokenName, getName());
	}
}
