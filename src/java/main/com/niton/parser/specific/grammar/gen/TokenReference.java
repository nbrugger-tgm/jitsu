package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class TokenReference {
	private SuperGrammarResult result;

	public TokenReference(SuperGrammarResult res) {
		this.result = res;
	}

	public String getTokenName() {
		if(result.getObject("token_name") == null) {
			return null;
		}
		return ((String)ResultResolver.getReturnValue(result.getObject("token_name")));
	}
}
