package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

import java.util.List;
import java.util.stream.Collectors;

public class Head {
	private final SuperNode result;

	public Head(SuperNode res) {
		this.result = res;
	}

	public List<IgnoringTokenDefiner> getTokenDefiners() {
		return (List<IgnoringTokenDefiner>) ((List<SuperNode>) ResultResolver.getReturnValue(
				result.getNode("token_definers"))).stream()
		                                          .map(res -> new IgnoringTokenDefiner(res))
		                                          .collect(Collectors.toList());
	}
}
