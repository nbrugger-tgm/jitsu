package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

public class IgnoringTokenDefiner {
	private final SuperGrammarResult result;

	public IgnoringTokenDefiner(SuperGrammarResult res) {
		this.result = res;
	}

	public Content getContent() {
		if (ResultResolver.getReturnValue(result.getObject("content")) == null) return null;
		return new Content((SuperGrammarResult) ResultResolver.getReturnValue(result.getObject(
				"content")));
	}
}
