package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;

public class ToIgnore {
	private final SuperGrammarResult result;

	public ToIgnore(SuperGrammarResult res) {
		this.result = res;
	}

	public AnyGrammarResult getIgnored() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("ignored"));
	}
}
