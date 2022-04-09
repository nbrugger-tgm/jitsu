package com.niton.parser.grammar.types;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.matchers.TokenMatcher;
import com.niton.parser.ast.TokenNode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class TokenGrammar extends Grammar<TokenMatcher, TokenNode> {
	private String tokenName;

	public TokenGrammar(String tokenName, String name) {
		super();
		this.tokenName = tokenName;
		setName(name);
	}

	public TokenGrammar(String token) {
		super();
		this.tokenName = token;
	}


	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public TokenMatcher createExecutor() {
		return new TokenMatcher(this);
	}

	@Override
	public void reconfigMatcher(@NotNull TokenMatcher tokenMatcher) {
		tokenMatcher.setTokenName(this);
	}

}
