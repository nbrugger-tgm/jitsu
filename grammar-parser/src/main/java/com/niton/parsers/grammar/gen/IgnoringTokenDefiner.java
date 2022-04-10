package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

public class IgnoringTokenDefiner {
	private final SuperNode result;

	public IgnoringTokenDefiner(SuperNode res) {
		this.result = res;
	}

	public Content getContent() {
		if (ResultResolver.getReturnValue(result.getNode("content")) == null) return null;
		return new Content((SuperNode) ResultResolver.getReturnValue(result.getNode(
				"content")));
	}
}
