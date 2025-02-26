package com.niton.jainparse.grammar.types;

import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.matchers.TokenMatcher;
import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.token.Tokenable;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class TokenGrammar<T extends Enum<T> & Tokenable> extends Grammar<TokenNode<T>,T> {
	private @Nullable T tokenName;

	public TokenGrammar(@Nullable T tokenName, String name) {
		this.tokenName = tokenName;
		setName(name);
	}

	public TokenGrammar(@Nullable T token) {
		this.tokenName = token;
	}


	@Override
	protected Grammar<?,T> copy() {
		return new TokenGrammar<>(tokenName, getName());
	}

	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public GrammarMatcher<TokenNode<T>,T> createExecutor() {
		return new TokenMatcher<>(this);
	}

	@Override
	public boolean isLeftRecursive(GrammarReference<T> ref) {
		return false;
	}


}
