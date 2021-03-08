package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class EnclosedExpression {
	private final SuperGrammarResult result;

	public EnclosedExpression(SuperGrammarResult res) {
		this.result = res;
	}

	public Expression getContent() {
		if(ResultResolver.getReturnValue(result.getObject("content")) == null)return null;return new Expression((SuperGrammarResult)ResultResolver.getReturnValue(result.getObject("content")));
	}

	public String toString() {
		return result.joinTokens();
	}
}
