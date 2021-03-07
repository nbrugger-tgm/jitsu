package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

import java.util.List;
import java.util.stream.Collectors;

public class RepeatIgnore {
	private final SuperGrammarResult result;

	public RepeatIgnore(SuperGrammarResult res) {
		this.result = res;
	}

	public List<ToIgnore> getIgnored() {
		return (List<ToIgnore>) ((List<SuperGrammarResult>) ResultResolver.getReturnValue(result.getObject(
				"ignored"))).stream().map(res -> new ToIgnore(res)).collect(Collectors.toList());
	}
}
