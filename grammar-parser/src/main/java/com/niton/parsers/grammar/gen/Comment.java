package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

public class Comment {
	private final SuperNode result;

	public Comment(SuperNode res) {
		this.result = res;
	}

	public String getMessage() {
		if (result.getNode("message") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getNode("message")));
	}
}
