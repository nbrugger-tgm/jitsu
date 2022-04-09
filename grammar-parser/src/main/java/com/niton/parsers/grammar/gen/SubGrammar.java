package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.AnyNode;
import com.niton.parser.ast.SuperNode;

public class SubGrammar {
	private final SuperNode result;

	public SubGrammar(SuperNode res) {
		this.result = res;
	}

	public String getOperation() {
		ResultResolver.setResolveAny(true);
		return (String) ResultResolver.getReturnValue(result.getNode("operation"));
	}

	public AnyNode getItem() {
		ResultResolver.setResolveAny(false);
		return (AnyNode) ResultResolver.getReturnValue(result.getNode("item"));
	}

	public String getRepeat() {
		if (result.getNode("repeat") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getNode("repeat")));
	}

	public NameAssignment getAssignment() {
		if (ResultResolver.getReturnValue(result.getNode("assignment")) == null) return null;
		return new NameAssignment((SuperNode) ResultResolver.getReturnValue(result.getNode(
				"assignment")));
	}
}
