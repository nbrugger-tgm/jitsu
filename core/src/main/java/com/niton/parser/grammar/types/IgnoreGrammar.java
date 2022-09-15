package com.niton.parser.grammar.types;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.matchers.IgnoreMatcher;
import com.niton.parser.ast.IgnoredNode;
import lombok.Getter;
import lombok.Setter;

/**
 * This Grammar ignores the given grammar
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class IgnoreGrammar extends Grammar<IgnoreMatcher, IgnoredNode> {
	private Grammar<?,?> grammar;

	public IgnoreGrammar(Grammar<?,?> grammar) {
		this.grammar = grammar;
	}


	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public IgnoreMatcher createExecutor() {
		return new IgnoreMatcher(grammar);
	}

}
