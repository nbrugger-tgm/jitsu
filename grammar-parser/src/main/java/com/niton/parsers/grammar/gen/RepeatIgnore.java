package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

import java.util.List;
import java.util.stream.Collectors;

public class RepeatIgnore {
	private final SuperNode result;

	public RepeatIgnore(SuperNode res) {
		this.result = res;
	}

	public List<ToIgnore> getIgnored() {
		return (List<ToIgnore>) ((List<SuperNode>) ResultResolver.getReturnValue(result.getNode(
				"ignored"))).stream().map(res -> new ToIgnore(res)).collect(Collectors.toList());
	}
}
