package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

public class Literal {
	private final SuperGrammarResult result;

	public Literal(SuperGrammarResult res) {
		this.result = res;
	}

	public String getRegex() {
		if (result.getObject("regex") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getObject("regex")));
	}
}
