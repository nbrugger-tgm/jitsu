package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.grammar.matchers.AnyExceptMatcher;
import com.niton.jainparse.token.Tokenable;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Stream;

/**
 * This grammar accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fulfills
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class AnyExceptGrammar<T extends Enum<T> & Tokenable> extends WrapperGrammar<TokenNode<T>,T>  {

	private Grammar<?,T> except;

	public AnyExceptGrammar(Grammar<?,T> grammarNotToAccept) {
		this.except = grammarNotToAccept;
	}


	@Override
	protected Grammar<?,T> copy() {
		return new AnyExceptGrammar<>(except);
	}

	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	protected GrammarMatcher<TokenNode<T>, T> createExecutor() {
		return new AnyExceptMatcher<>(except);
	}

	@Override
	public boolean isLeftRecursive(GrammarReference<T> ref) {
		return except.isLeftRecursive(ref);
	}


	@Override
	protected Stream<Grammar<?,T>> getWrapped() {
		return Stream.of(except);
	}
}
