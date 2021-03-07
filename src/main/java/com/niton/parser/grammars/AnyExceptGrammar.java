package com.niton.parser.grammars;

import com.niton.parser.Grammar;
import com.niton.parser.matchers.AnyExceptMatcher;
import com.niton.parser.result.TokenGrammarResult;

/**
 * This grammar accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fullfilles
 *
 * @author Nils
 * @version 2019-05-29
 */
public class AnyExceptGrammar extends Grammar<AnyExceptMatcher, TokenGrammarResult> {

	private Grammar dunnoaccept;

	public AnyExceptGrammar(Grammar grammarNotToAccept) {
		this.dunnoaccept = grammarNotToAccept;
	}

	/**
	 * @return the dunnoaccept
	 */
	public Grammar getDunnoaccept() {
		return dunnoaccept;
	}

	/**
	 * @param dunnoaccept the dunnoaccept to set
	 */
	public void setDunnoaccept(Grammar dunnoaccept) {
		this.dunnoaccept = dunnoaccept;
	}

	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public AnyExceptMatcher createExecutor() {
		return new AnyExceptMatcher(dunnoaccept);
	}

	@Override
	public void reconfigMatcher(AnyExceptMatcher anyExceptMatcher) {
		anyExceptMatcher.setDunnoaccept(dunnoaccept);
	}
}
