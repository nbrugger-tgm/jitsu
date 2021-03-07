package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

import java.util.List;
import java.util.stream.Collectors;

public class Head {
	private final SuperGrammarResult result;

	public Head(SuperGrammarResult res) {
		this.result = res;
	}

	public List<IgnoringTokenDefiner> getTokenDefiners() {
		return (List<IgnoringTokenDefiner>) ((List<SuperGrammarResult>) ResultResolver.getReturnValue(
				result.getObject("token_definers"))).stream()
		                                            .map(res -> new IgnoringTokenDefiner(res))
		                                            .collect(Collectors.toList());
	}
}
