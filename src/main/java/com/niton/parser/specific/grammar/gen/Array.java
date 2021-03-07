package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

import java.util.List;
import java.util.stream.Collectors;

public class Array {
	private final SuperGrammarResult result;

	public Array(SuperGrammarResult res) {
		this.result = res;
	}

	public List<ArrayItem> getItems() {
		return (List<ArrayItem>) ((List<SuperGrammarResult>) ResultResolver.getReturnValue(result.getObject(
				"items"))).stream().map(res -> new ArrayItem(res)).collect(Collectors.toList());
	}
}
