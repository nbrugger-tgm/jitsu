package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class Addition {
	private final SuperGrammarResult result;

	public Addition(SuperGrammarResult res) {
		this.result = res;
	}

	public AdditionalOperand getOperand() {
		if(ResultResolver.getReturnValue(result.getObject("operand")) == null)return null;return new AdditionalOperand((SuperGrammarResult)ResultResolver.getReturnValue(result.getObject("operand")));
	}

	public AnyGrammarResult getSummand() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("summand"));
	}

	public String toString() {
		return result.joinTokens();
	}
}
