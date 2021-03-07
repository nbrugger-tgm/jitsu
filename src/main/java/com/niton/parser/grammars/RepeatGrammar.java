package com.niton.parser.grammars;

import com.niton.parser.Grammar;
import com.niton.parser.matchers.RepeatMatcher;
import com.niton.parser.result.ListGrammarResult;

/**
 * Checks the grammar as often as is ocures
 *
 * @author Nils
 * @version 2019-05-29
 */
public class RepeatGrammar extends Grammar<RepeatMatcher, ListGrammarResult> {
	private Grammar check;

	public RepeatGrammar(Grammar gramarReference) {
		this.check = gramarReference;
	}


	/**
	 * @return the check
	 */
	public Grammar getCheck() {
		return check;
	}

	/**
	 * @param check the check to set
	 */
	public void setCheck(Grammar check) {
		this.check = check;
	}


	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public RepeatMatcher createExecutor() {
		return new RepeatMatcher(check);
	}

	@Override
	public void reconfigMatcher(RepeatMatcher repeatMatcher) {
		repeatMatcher.setCheck(check);
	}

}
