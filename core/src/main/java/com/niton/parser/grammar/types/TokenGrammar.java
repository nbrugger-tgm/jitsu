package com.niton.parser.grammar.types;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.matchers.TokenMatcher;
import com.niton.parser.ast.TokenNode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

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


}
