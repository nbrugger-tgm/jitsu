package com.niton.parser.grammars;

import com.niton.parser.Grammar;
import com.niton.parser.GrammarReference;
import com.niton.parser.matchers.ReferenceGrammarMatcher;
import com.niton.parser.GrammarResult;

/**
 * This is the GrammarMatchGrammar Class
 * 
 * @author Nils Brugger
 * @version 2019-06-05
 */
public class GrammarReferenceGrammar extends Grammar<ReferenceGrammarMatcher,GrammarResult> {
	private String grammar;

	public GrammarReferenceGrammar(String g) {
		this.grammar = g;
	}

	/**
	 * @return the grammar
	 */
	public String getGrammar() {
		return grammar;
	}
	public Grammar grammer(GrammarReference ref){
		return ref.get(grammar);
	}

	/**
	 * @param grammar the grammar to set
	 */
	public void setGrammar(String grammar) {
		this.grammar = grammar;
	}

	/**
	 * @see Grammar#createExecutor()
	 * @return
	 */
	@Override
	public ReferenceGrammarMatcher createExecutor() {
		return new ReferenceGrammarMatcher(grammar);
	}

	@Override
	public void reconfigMatcher(ReferenceGrammarMatcher referenceGrammarMatcher) {
		referenceGrammarMatcher.setGrammar(grammar);
	}

	@Override
	public String getName() {
		return grammar;
	}

}
