package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;

import java.util.List;

public class FactorExpression {
	private SuperGrammarResult result;

	public FactorExpression(SuperGrammarResult res) {
		this.result = res;
	}

	public AnyGrammarResult getFirstExpression() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("first_expression"));
	}

	public FactorOperand getOperand1() {
		return new FactorOperand(result.getObject("operand1"));
	}

	public List<FactorOperand> getOperands() {
		return ((List<FactorOperand>) ResultResolver.getReturnValue(result.getObject("operands")));
	}
}
