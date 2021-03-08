package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class NumericExpression {
	private final SuperGrammarResult result;

	public NumericExpression(SuperGrammarResult res) {
		this.result = res;
	}

	public AnyGrammarResult getContent() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("content"));
	}

	public String toString() {
		return result.joinTokens();
	}
}
