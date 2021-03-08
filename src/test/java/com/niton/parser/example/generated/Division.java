package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class Division {
	private final SuperGrammarResult result;

	public Division(SuperGrammarResult res) {
		this.result = res;
	}

	public AnyGrammarResult getFactor1() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("factor1"));
	}

	public String getOperand() {
		if(result.getObject("operand") == null) {
			return null;
		}
		return ((String)ResultResolver.getReturnValue(result.getObject("operand")));
	}

	public AnyGrammarResult getFactor2() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("factor2"));
	}

	public String toString() {
		return result.joinTokens();
	}
}
