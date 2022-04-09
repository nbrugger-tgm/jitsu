package com.niton.parsers.grammar;

import com.niton.parser.grammar.GrammarReferenceMap;
import com.niton.parser.token.TokenPattern;
import com.niton.parser.token.Tokenable;
import com.niton.parser.token.GenericToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is the GrammarResult Class
 *
 * @author Nils Brugger
 * @version 2019-06-12
 */
public class GrammarFileContent {
	public GrammarReferenceMap       grammars = new GrammarReferenceMap();
	public Map<String, TokenPattern> tokens   = new HashMap<>();

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
	public Map<String, TokenPattern> getTokenMap() {
		return tokens;
	}

	/**
	 * @param tokens the tokens to set
	 */
	public void setTokens(Map<String, TokenPattern> tokens) {
		this.tokens = tokens;
	}

	public List<Tokenable> getTokens() {
		return tokens.entrySet().stream().map(e-> new GenericToken(e.getValue(),e.getKey())).collect(
				Collectors.toList());
	}
}

