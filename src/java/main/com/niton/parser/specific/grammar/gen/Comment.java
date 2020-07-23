package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class Comment {
	private SuperGrammarResult result;

	public Comment(SuperGrammarResult res) {
		this.result = res;
	}

	public String getMessage() {
		if(result.getObject("message") == null) {
			return null;
		}
		return ((String)ResultResolver.getReturnValue(result.getObject("message")));
	}
}
