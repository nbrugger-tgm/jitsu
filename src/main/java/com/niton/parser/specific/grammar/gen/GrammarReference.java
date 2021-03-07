package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

public class GrammarReference {
	private final SuperGrammarResult result;

	public GrammarReference(SuperGrammarResult res) {
		this.result = res;
	}

	public String getGrammarName() {
		if (result.getObject("grammar_name") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getObject("grammar_name")));
	}
}
