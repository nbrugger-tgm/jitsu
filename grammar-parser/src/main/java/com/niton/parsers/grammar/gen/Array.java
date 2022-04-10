package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

import java.util.List;
import java.util.stream.Collectors;

public class Array {
	private final SuperNode result;

	public Array(SuperNode res) {
		this.result = res;
	}

	public List<ArrayItem> getItems() {
		return (List<ArrayItem>) ((List<SuperNode>) ResultResolver.getReturnValue(result.getNode(
				"items"))).stream().map(res -> new ArrayItem(res)).collect(Collectors.toList());
	}
}
