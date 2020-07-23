package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class TokenDefiner {
	private SuperGrammarResult result;

	public TokenDefiner(SuperGrammarResult res) {
		this.result = res;
	}

	public String getName() {
		if(result.getObject("name") == null) {
			return null;
		}
		return ((String)ResultResolver.getReturnValue(result.getObject("name")));
	}

	public TokenLiteral getLiteral() {
		return new TokenLiteral(result.getObject("literal"));
	}
}
