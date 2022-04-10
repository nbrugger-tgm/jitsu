package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.AnyNode;
import com.niton.parser.ast.SuperNode;

public class ToIgnore {
	private final SuperNode result;

	public ToIgnore(SuperNode res) {
		this.result = res;
	}

	public AnyNode getIgnored() {
		ResultResolver.setResolveAny(false);
		return (AnyNode) ResultResolver.getReturnValue(result.getNode("ignored"));
	}
}
