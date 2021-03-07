package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;

public class Expression {
	private SuperGrammarResult result;

	public Expression(SuperGrammarResult res) {
		this.result = res;
	}

	public AnyGrammarResult getContent() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("content"));
	}
}
