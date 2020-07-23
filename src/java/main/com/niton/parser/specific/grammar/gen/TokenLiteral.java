package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class TokenLiteral {
	private SuperGrammarResult result;

	public TokenLiteral(SuperGrammarResult res) {
		this.result = res;
	}

	public String getRegex() {
		if(result.getObject("regex") == null) {
			return null;
		}
		return ((String)ResultResolver.getReturnValue(result.getObject("regex")));
	}
}
