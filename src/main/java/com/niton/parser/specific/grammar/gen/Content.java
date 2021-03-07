package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

public class Content {
	private final SuperGrammarResult result;

	public Content(SuperGrammarResult res) {
		this.result = res;
	}

	public String getName() {
		if (result.getObject("name") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getObject("name")));
	}

	public Literal getLiteral() {
		if (ResultResolver.getReturnValue(result.getObject("literal")) == null) return null;
		return new Literal((SuperGrammarResult) ResultResolver.getReturnValue(result.getObject(
				"literal")));
	}
}
