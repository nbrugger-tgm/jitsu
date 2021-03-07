package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;

import java.util.List;

public class FileHead {
	private SuperGrammarResult result;

	public FileHead(SuperGrammarResult res) {
		this.result = res;
	}

	public List<IgnoringTokenDefiner> getTokenDefiners() {
		return ((List<IgnoringTokenDefiner>) ResultResolver.getReturnValue(result.getObject(
				"token_definers")));
	}
}
