package com.niton.parser.grammars;

import com.niton.parser.Grammar;
import com.niton.parser.matchers.AnyOfMatcher;
import com.niton.parser.result.AnyGrammarResult;

/**
 * Checks agains all given Grammars syncron and returns the first matching
 *
 * @author Nils
 * @version 2019-05-29
 */
public class MultiGrammar extends Grammar<AnyOfMatcher, AnyGrammarResult> {
	private Grammar[] tokens;

	public MultiGrammar(Grammar[] grammars) {
		tokens = grammars;
	}

	/**
	 * @return the tokens
	 */
	public Grammar[] getGrammars() {
		return tokens;
	}

	/**
	 * @param tokens the tokens to set
	 */
	public void setTokens(Grammar[] tokens) {
		this.tokens = tokens;
	}

	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public AnyOfMatcher createExecutor() {
		return new AnyOfMatcher(this);
	}

	@Override
	public void reconfigMatcher(AnyOfMatcher multiMatcher) {
		multiMatcher.setGrammars(this);
	}
}
