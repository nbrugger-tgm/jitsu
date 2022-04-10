package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

public class Content {
	private final SuperNode result;

	public Content(SuperNode res) {
		this.result = res;
	}

	public String getName() {
		if (result.getNode("name") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getNode("name")));
	}

	public Literal getLiteral() {
		if (ResultResolver.getReturnValue(result.getNode("literal")) == null) return null;
		return new Literal((SuperNode) ResultResolver.getReturnValue(result.getNode(
				"literal")));
	}
}
