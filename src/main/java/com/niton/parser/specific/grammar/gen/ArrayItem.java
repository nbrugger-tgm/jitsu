package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;

public class ArrayItem {
	private final SuperGrammarResult result;

	public ArrayItem(SuperGrammarResult res) {
		this.result = res;
	}

	public AnyGrammarResult getItem() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("item"));
	}

	public String getSeperator() {
		ResultResolver.setResolveAny(true);
		return (String) ResultResolver.getReturnValue(result.getObject("seperator"));
	}
}
