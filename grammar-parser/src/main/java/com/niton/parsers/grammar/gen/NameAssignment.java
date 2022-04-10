package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

public class NameAssignment {
	private final SuperNode result;

	public NameAssignment(SuperNode res) {
		this.result = res;
	}

	public String getName() {
		if (result.getNode("name") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getNode("name")));
	}
}
