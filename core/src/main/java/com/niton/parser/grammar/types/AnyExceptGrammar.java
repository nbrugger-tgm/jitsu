package com.niton.parser.grammar.types;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.matchers.AnyExceptMatcher;
import lombok.Getter;
import lombok.Setter;

/**
 * This grammar accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fulfills
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class AnyExceptGrammar extends Grammar<TokenNode> implements GrammarReference.Single {

	private Grammar<?> except;

	public AnyExceptGrammar(Grammar<?> grammarNotToAccept) {
		this.except = grammarNotToAccept;
	}


	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	protected AnyExceptMatcher createExecutor() {
		return new AnyExceptMatcher(except);
	}

	@Override
	public Grammar<?> getGrammar() {
		return except;
	}

}
