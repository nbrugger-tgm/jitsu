package com.niton.parser.grammar.types;

import com.niton.parser.ast.SequenceNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.matchers.RepeatMatcher;
import lombok.Getter;
import lombok.Setter;

/**
 * Checks the grammar as often as is occurs
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class RepeatGrammar extends Grammar<SequenceNode> implements GrammarReference.Single {
	private Grammar<?> check;

	public RepeatGrammar(Grammar<?> gramarReference) {
		this.check = gramarReference;
	}


	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public RepeatMatcher createExecutor() {
		return new RepeatMatcher(check);
	}

	@Override
	public Grammar<?> getGrammar() {
		return check;
	}
}
