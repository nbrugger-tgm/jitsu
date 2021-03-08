package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class FactorOperand {
	private final SuperGrammarResult result;

	public FactorOperand(SuperGrammarResult res) {
		this.result = res;
	}

	public String getOperator() {
		ResultResolver.setResolveAny(true);
		return (String) ResultResolver.getReturnValue(result.getObject("operator"));
	}

	public String toString() {
		return result.joinTokens();
	}
}
