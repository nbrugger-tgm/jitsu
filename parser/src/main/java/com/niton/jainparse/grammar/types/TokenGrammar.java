package com.niton.jainparse.grammar.types;

import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.matchers.TokenMatcher;
import com.niton.jainparse.ast.TokenNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenGrammar extends Grammar<TokenNode> {
	private String tokenName;

	public TokenGrammar(String tokenName, String name) {
		super();
		this.tokenName = tokenName;
		setName(name);
	}

	public TokenGrammar(String token) {
		super();
		this.tokenName = token;
//		setName(token);
	}


	@Override
	protected Grammar<?> copy() {
		return new TokenGrammar(tokenName, getName());
	}

	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public TokenMatcher createExecutor() {
		return new TokenMatcher(this);
	}

	@Override
	public boolean isLeftRecursive(GrammarReference ref) {
		return false;
	}


}
