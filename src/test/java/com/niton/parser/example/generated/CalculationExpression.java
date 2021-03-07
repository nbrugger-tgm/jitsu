package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;

public class CalculationExpression {
	private SuperGrammarResult result;

	public CalculationExpression(SuperGrammarResult res) {
		this.result = res;
	}

	public AnyGrammarResult getContent() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("content"));
	}
}
