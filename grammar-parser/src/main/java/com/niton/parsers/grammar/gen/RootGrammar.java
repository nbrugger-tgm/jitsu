package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

import java.util.List;
import java.util.stream.Collectors;

public class RootGrammar {
	private final SuperNode result;

	public RootGrammar(SuperNode res) {
		this.result = res;
	}

	public String getName() {
		if (result.getNode("name") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getNode("name")));
	}

	public List<SubGrammar> getChain() {
		return (List<SubGrammar>) ((List<SuperNode>) ResultResolver.getReturnValue(result.getNode(
				"chain"))).stream().map(res -> new SubGrammar(res)).collect(Collectors.toList());
	}
}
