package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.AnyNode;
import com.niton.parser.ast.SuperNode;

public class ArrayItem {
	private final SuperNode result;

	public ArrayItem(SuperNode res) {
		this.result = res;
	}

	public AnyNode getItem() {
		ResultResolver.setResolveAny(false);
		return (AnyNode) ResultResolver.getReturnValue(result.getNode("item"));
	}

	public String getSeperator() {
		ResultResolver.setResolveAny(true);
		return (String) ResultResolver.getReturnValue(result.getNode("seperator"));
	}
}
