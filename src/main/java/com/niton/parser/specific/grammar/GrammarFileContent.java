package com.niton.parser.specific.grammar;

import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.Token;
import com.niton.parser.Tokenable;
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
	public GrammarReferenceMap grammars = new GrammarReferenceMap();
	public Map<String, Token>  tokens   = new HashMap<>();

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
	public Map<String, Token> getTokenMap() {
		return tokens;
	}

	/**
	 * @param tokens the tokens to set
	 */
	public void setTokens(Map<String, Token> tokens) {
		this.tokens = tokens;
	}

	public List<Tokenable> getTokens() {
		return tokens.entrySet().stream().map(e-> new GenericToken(e.getValue(),e.getKey())).collect(
				Collectors.toList());
	}
}

