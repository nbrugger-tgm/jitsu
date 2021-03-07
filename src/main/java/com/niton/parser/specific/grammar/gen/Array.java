package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.util.List;

public class Array {
	private SuperGrammarResult result;

	public Array(SuperGrammarResult res) {
		this.result = res;
	}

	public List<ArrayItem> getItems() {
		return ((List<ArrayItem>)ResultResolver.getReturnValue(result.getObject("items")));
	}
}
