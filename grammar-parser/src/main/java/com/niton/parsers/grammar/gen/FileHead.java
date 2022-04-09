package com.niton.parsers.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.ast.SuperNode;

import java.util.List;

public class FileHead {
	private SuperNode result;

	public FileHead(SuperNode res) {
		this.result = res;
	}

	public List<IgnoringTokenDefiner> getTokenDefiners() {
		return ((List<IgnoringTokenDefiner>) ResultResolver.getReturnValue(result.getNode(
				"token_definers")));
	}
}
