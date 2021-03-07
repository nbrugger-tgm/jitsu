package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;

import java.util.List;

public class AdditionalExpression {
	private SuperGrammarResult result;

	public AdditionalExpression(SuperGrammarResult res) {
		this.result = res;
	}

	public AnyGrammarResult getFirstExpression() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("first_expression"));
	}

	public AdditionalOperand getOperand1() {
		return new AdditionalOperand(result.getObject("operand1"));
	}

	public List<AdditionalOperand> getOperands() {
		return ((List<AdditionalOperand>) ResultResolver.getReturnValue(result.getObject("operands")));
	}
}
