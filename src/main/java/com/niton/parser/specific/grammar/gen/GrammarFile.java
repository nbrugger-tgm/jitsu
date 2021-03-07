package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

import java.util.List;
import java.util.stream.Collectors;

public class GrammarFile {
	private final SuperGrammarResult result;

	public GrammarFile(SuperGrammarResult res) {
		this.result = res;
	}

	public Head getHead() {
		if (ResultResolver.getReturnValue(result.getObject("head")) == null) return null;
		return new Head((SuperGrammarResult) ResultResolver.getReturnValue(result.getObject("head")));
	}

	public List<RootGrammar> getGrammars() {
		return (List<RootGrammar>) ((List<SuperGrammarResult>) ResultResolver.getReturnValue(result.getObject(
				"grammars"))).stream()
		                     .map(res -> new RootGrammar(res))
		                     .collect(Collectors.toList());
	}
}
