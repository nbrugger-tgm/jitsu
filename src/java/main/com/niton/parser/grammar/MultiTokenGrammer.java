package com.niton.parser.grammar;

import java.util.ArrayList;
import java.util.Arrays;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;
import com.niton.parser.grammar.exectors.GrammarExecutor;
import com.niton.parser.grammar.exectors.MultiTokenExecutor;

/**
 * Expects one of the tokens given
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class MultiTokenGrammer extends Grammar {
	private String[] tokens;

	public MultiTokenGrammer(String[] tokens, String name) {
		this.tokens = tokens;
		setName(name);
	}


	/**
	 * @return the tokens
	 */
	public String[] getTokens() {
		return tokens;
	}

	/**
	 * @param tokens the tokens to set
	 */
	public void setTokens(String[] tokens) {
		this.tokens = tokens;
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
		return new MultiTokenExecutor(tokens, getName());
	}
	
}
