package com.niton.parser.grammar.types;

import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.ast.AstNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.matchers.ReferenceGrammarMatcher;
import lombok.Getter;
import lombok.Setter;

/**
 * This is the GrammarMatchGrammar Class
 *
 * @author Nils Brugger
 * @version 2019-06-05
 */
@Getter
@Setter
public class GrammarReferenceGrammar extends Grammar<ReferenceGrammarMatcher, AstNode> {
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

	/**
	 * @param grammar the grammar to set
	 */
	public void setGrammar(String grammar) {
		this.grammar = grammar;
	}

	public Grammar<?,?> grammer(GrammarReference ref) {
		return ref.get(grammar);
	}

	/**
	 * @return
	 * @see Grammar#createExecutor()
	 */
	@Override
	public ReferenceGrammarMatcher createExecutor() {
		return new ReferenceGrammarMatcher(grammar);
	}

    @Override
	public String getName() {
		return getGrammar();
	}

}
