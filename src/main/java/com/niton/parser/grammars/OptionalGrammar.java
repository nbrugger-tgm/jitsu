package com.niton.parser.grammars;

import com.niton.parser.Grammar;
import com.niton.parser.matchers.OptionalMatcher;
import com.niton.parser.result.OptionalGrammarResult;

/**
 * Cheks if the grammar is right if yes it adds the element to the output if not
 * it is ignored
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class OptionalGrammar extends Grammar<OptionalMatcher, OptionalGrammarResult> {
	private Grammar check;

    public OptionalGrammar(Grammar grammarReferenceGrammar) {
        check = grammarReferenceGrammar;
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
	public OptionalMatcher createExecutor() {
		return new OptionalMatcher(check);
	}

	@Override
	public void reconfigMatcher(OptionalMatcher optionalMatcher) {
		optionalMatcher.setCheck(check);
	}
}
