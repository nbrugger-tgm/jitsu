package com.niton.parser.grammars;

import com.niton.parser.Grammar;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.matchers.MultiMatcher;

/**
 * Checks agains all given Grammars syncron and returns the first matching
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class MultiGrammar extends Grammar<MultiMatcher,AnyGrammarResult> {
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
	public MultiMatcher createExecutor() {
		return new MultiMatcher(this);
	}

	@Override
	public void reconfigMatcher(MultiMatcher multiMatcher) {
		multiMatcher.setGrammars(this);
	}
}
