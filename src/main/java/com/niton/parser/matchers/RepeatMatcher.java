package com.niton.parser.matchers;

import com.niton.parser.GrammarMatcher;
import com.niton.parser.token.TokenStream;
import com.niton.parser.Grammar;
import com.niton.parser.GrammarResult;
import com.niton.parser.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.result.ListGrammarResult;

/**
 * Checks the grammar as often as is ocures
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class RepeatMatcher extends GrammarMatcher<ListGrammarResult> {
	
	private Grammar check;

	public RepeatMatcher(Grammar expression) {
		this.check = expression;
	}

	/**
	 * @see Grammar#process(java.util.ArrayList)
	 */
	@Override
	public ListGrammarResult process(TokenStream tokens, GrammarReference ref) throws ParsingException {
		boolean keep = true;
		ListGrammarResult obj = new ListGrammarResult();
		while (keep) {
			try {
				GrammarResult gr = check.parse(tokens, ref);
				obj.add(gr);
				tokens.commit();
			} catch (ParsingException e) {
				tokens.rollback();
				return obj;
			}
		}
		return obj;
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
