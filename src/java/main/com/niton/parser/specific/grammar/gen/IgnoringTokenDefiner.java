package com.niton.parser.specific.grammar.gen;

import com.niton.parser.result.SuperGrammarResult;

public class IgnoringTokenDefiner {
	private SuperGrammarResult result;

	public IgnoringTokenDefiner(SuperGrammarResult res) {
		this.result = res;
	}

	public TokenDefiner getContent() {
		return new TokenDefiner(result.getObject("content"));
	}
}
