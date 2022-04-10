package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

public class GrammarReference {
	private final SuperNode result;

	public GrammarReference(SuperNode res) {
		this.result = res;
	}

	public String getGrammarName() {
		if (result.getNode("grammar_name") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getNode("grammar_name")));
	}
}
