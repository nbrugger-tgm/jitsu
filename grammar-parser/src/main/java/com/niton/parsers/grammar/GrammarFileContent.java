package com.niton.parsers.grammar;

import com.niton.jainparse.grammar.api.GrammarReferenceMap;
import com.niton.jainparse.token.Tokenable;

import java.util.Set;

/**
 * This is the GrammarResult Class
 *
 * @author Nils Brugger
 * @version 2019-06-12
 */
public class GrammarFileContent {
	private GrammarReferenceMap grammars = new GrammarReferenceMap();
	private Set<Tokenable>      tokens;

	GrammarFileContent(){}

	/**
	 * @return the grammars
	 */
	public GrammarReferenceMap getGrammars() {
		return grammars;
	}

	/**
	 * @param grammars the grammars to set
	 */
	void setGrammars(GrammarReferenceMap grammars) {
		this.grammars = grammars;
	}

	/**
	 * @param tokens the tokens to set
	 */
	void setTokens(Set<Tokenable> tokens) {
		this.tokens = tokens;
	}

	public Set<Tokenable> getTokens() {
		return tokens;
	}
}

