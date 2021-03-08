package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class Signednumber {
	private final SuperGrammarResult result;

	public Signednumber(SuperGrammarResult res) {
		this.result = res;
	}

	public String getSign() {
		ResultResolver.setResolveAny(true);
		return (String) ResultResolver.getReturnValue(result.getObject("sign"));
	}

	public Number getNumber() {
		if(ResultResolver.getReturnValue(result.getObject("number")) == null)return null;return new Number((SuperGrammarResult)ResultResolver.getReturnValue(result.getObject("number")));
	}

	public String toString() {
		return result.joinTokens();
	}
}
