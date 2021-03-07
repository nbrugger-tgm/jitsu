package com.niton.parser.example.generated;

import com.niton.parser.result.SuperGrammarResult;

public class EnclosedExpression {
	private SuperGrammarResult result;

	public EnclosedExpression(SuperGrammarResult res) {
		this.result = res;
	}

	public Expression getContent() {
		return new Expression(result.getObject("content"));
	}
}
