package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;
import java.util.List;

public class RootGrammar {
	private SuperGrammarResult result;

	public RootGrammar(SuperGrammarResult res) {
		this.result = res;
	}

	public String getName() {
		if(result.getObject("name") == null) {
			return null;
		}
		return ((String)ResultResolver.getReturnValue(result.getObject("name")));
	}

	public List<SubGrammar> getChain() {
		return ((List<SubGrammar>)ResultResolver.getReturnValue(result.getObject("chain")));
	}
}
