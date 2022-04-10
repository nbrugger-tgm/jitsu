package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

public class TokenReference {
	private final SuperNode result;

	public TokenReference(SuperNode res) {
		this.result = res;
	}

	public String getTokenName() {
		if (result.getNode("token_name") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getNode("token_name")));
	}
}
