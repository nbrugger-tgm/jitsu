package com.niton.parser.matchers;

import com.niton.parser.GrammarMatcher;
import com.niton.parser.token.TokenStream;
import com.niton.parser.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.Grammar;
import com.niton.parser.result.OptionalGrammarResult;

/**
 * Cheks if the grammar is right if yes it adds the element to the output if not
 * it is ignored
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class OptionalMatcher extends GrammarMatcher<OptionalGrammarResult> {
	
	private Grammar check;

	public OptionalMatcher(Grammar value) {
		this.check = value;
	}

	/**
	 * @see Grammar#process(java.util.List)
	 */
	@Override
	public OptionalGrammarResult process(TokenStream tokens, GrammarReference ref) throws ParsingException {
		try {
			OptionalGrammarResult obj = new OptionalGrammarResult();
			obj.setValue(check.parse(tokens, ref));
			tokens.rollback();
			return obj;
		} catch (ParsingException e) {
			tokens.rollback();
			return new OptionalGrammarResult();
		}
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

}
