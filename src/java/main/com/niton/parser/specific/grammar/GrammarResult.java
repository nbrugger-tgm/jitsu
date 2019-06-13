package com.niton.parser.specific.grammar;

import java.util.HashMap;
import java.util.LinkedList;

import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.Token;

/**
 * This is the GrammarResult Class
 * @author Nils Brugger
 * @version 2019-06-12
 */
public class GrammarResult {
	public GrammarReferenceMap grammars = new GrammarReferenceMap();
	public HashMap<String,Token> tokens = new HashMap<>();
	/**
	 * @return the grammars
	 */
	public GrammarReferenceMap getGrammars() {
		return grammars;
	}
	/**
	 * @param grammars the grammars to set
	 */
	public void setGrammars(GrammarReferenceMap grammars) {
		this.grammars = grammars;
	}
	/**
	 * @return the tokens
	 */
	public HashMap<String, Token> getTokens() {
		return tokens;
	}
	/**
	 * @param tokens the tokens to set
	 */
	public void setTokens(HashMap<String, Token> tokens) {
		this.tokens = tokens;
	}
	
}

