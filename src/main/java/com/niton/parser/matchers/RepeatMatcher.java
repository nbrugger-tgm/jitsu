package com.niton.parser.matchers;

import com.niton.parser.Grammar;
import com.niton.parser.GrammarMatcher;
import com.niton.parser.GrammarReference;
import com.niton.parser.GrammarResult;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.result.ListGrammarResult;
import com.niton.parser.token.TokenStream;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Checks the grammar as often as is ocures
 *
 * @author Nils
 * @version 2019-05-29
 */
public class RepeatMatcher extends GrammarMatcher<ListGrammarResult> {

	private Grammar          check;
	private ParsingException interrupt;

	public RepeatMatcher(Grammar expression) {
		this.check = expression;
	}

	@Override
	public ParsingException getLastException() {
		return interrupt;
	}

	/**
	 * @see GrammarMatcher#process(TokenStream, GrammarReference)
	 */
	@Override
	public ListGrammarResult process(TokenStream tokens, GrammarReference ref)
	throws ParsingException {
		boolean           keep = true;
		ListGrammarResult obj  = new ListGrammarResult();
		while (keep) {
			try {
				GrammarResult gr = check.parse(tokens, ref);
				obj.add(gr);
				tokens.commit();
			} catch (ParsingException e) {
				tokens.rollback();
				interrupt = new ParsingException(this.check + " no new match (index=" + obj.getList()
				                                                                           .size() + ") found -> \n" + Arrays
						.stream(
								e.getMessage().split("\n"))
						.map(l -> "\t" + l)
						.collect(Collectors.joining("\n")), e);
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
