package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

public class TokenLiteral {
	private SuperNode result;

	public TokenLiteral(SuperNode res) {
		this.result = res;
	}

	public String getRegex() {
		if (result.getNode("regex") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getNode("regex")));
	}
}
