package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

public class Signednumber {
	private SuperGrammarResult result;

	public Signednumber(SuperGrammarResult res) {
		this.result = res;
	}

	public String getSign() {
		ResultResolver.setResolveAny(true);
		return (String) ResultResolver.getReturnValue(result.getObject("sign"));
	}

	public Number getNumber() {
		return new Number(result.getObject("number"));
	}
}
