package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

import java.util.List;
import java.util.stream.Collectors;

public class RootGrammar {
	private final SuperGrammarResult result;

	public RootGrammar(SuperGrammarResult res) {
		this.result = res;
	}

	public String getName() {
		if (result.getObject("name") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getObject("name")));
	}

	public List<SubGrammar> getChain() {
		return (List<SubGrammar>) ((List<SuperGrammarResult>) ResultResolver.getReturnValue(result.getObject(
				"chain"))).stream().map(res -> new SubGrammar(res)).collect(Collectors.toList());
	}
}
