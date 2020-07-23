package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;

public class NameAssignment {
	private SuperGrammarResult result;

	public NameAssignment(SuperGrammarResult res) {
		this.result = res;
	}

	public String getName() {
		if(result.getObject("name") == null) {
			return null;
		}
		return ((String)ResultResolver.getReturnValue(result.getObject("name")));
	}
}
