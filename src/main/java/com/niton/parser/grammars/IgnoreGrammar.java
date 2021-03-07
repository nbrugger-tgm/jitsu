package com.niton.parser.grammars;

import com.niton.parser.Grammar;
import com.niton.parser.result.IgnoredGrammarResult;
import com.niton.parser.matchers.IgnoreMatcher;

/**
 * This Grammar ignores the given grammar
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class IgnoreGrammar extends Grammar<IgnoreMatcher,IgnoredGrammarResult> {
	private Grammar grammar;

	public IgnoreGrammar(Grammar grammar) {
		this.grammar = grammar;
	}

	public Grammar getGrammar() {
		return grammar;
	}

	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public IgnoreMatcher createExecutor() {
		return new IgnoreMatcher(grammar);
	}

	@Override
	public void reconfigMatcher(IgnoreMatcher ignoreMatcher) {
		ignoreMatcher.setGrammar(grammar);
	}
}
