package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

import java.util.List;
import java.util.stream.Collectors;

public class GrammarFile {
	private final SuperNode result;

	public GrammarFile(SuperNode res) {
		this.result = res;
	}

	public Head getHead() {
		if (ResultResolver.getReturnValue(result.getNode("head")) == null) return null;
		return new Head((SuperNode) ResultResolver.getReturnValue(result.getNode("head")));
	}

	public List<RootGrammar> getGrammars() {
		return (List<RootGrammar>) ((List<SuperNode>) ResultResolver.getReturnValue(result.getNode(
				"grammars"))).stream()
		                     .map(res -> new RootGrammar(res))
		                     .collect(Collectors.toList());
	}
}
