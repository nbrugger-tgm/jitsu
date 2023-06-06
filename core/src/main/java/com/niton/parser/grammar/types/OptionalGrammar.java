package com.niton.parser.grammar.types;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.matchers.OptionalMatcher;
import com.niton.parser.ast.OptionalNode;
import lombok.Getter;
import lombok.Setter;

/**
 * Cheks if the grammar is right if yes it adds the element to the output if not
 * it is ignored
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class OptionalGrammar extends Grammar<OptionalNode> implements GrammarReference.Single {
	private Grammar<?> check;

	public OptionalGrammar(Grammar<?> grammarReferenceGrammar) {
		check = grammarReferenceGrammar;
	}


	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public OptionalMatcher createExecutor() {
		return new OptionalMatcher(check);
	}

	@Override
	public Grammar<?> getGrammar() {
		return check;
	}
}
