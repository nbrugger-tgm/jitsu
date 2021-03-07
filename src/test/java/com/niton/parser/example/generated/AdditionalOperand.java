package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

public class AdditionalOperand {
	private SuperGrammarResult result;

	public AdditionalOperand(SuperGrammarResult res) {
		this.result = res;
	}

	public String getOperator() {
		ResultResolver.setResolveAny(true);
		return (String) ResultResolver.getReturnValue(result.getObject("operator"));
	}

	public Expression getSecondExpression() {
		return new Expression(result.getObject("second_expression"));
	}
}
