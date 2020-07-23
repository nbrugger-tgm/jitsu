package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.util.List;

public class RepeatIgnore {
	private SuperGrammarResult result;

	public RepeatIgnore(SuperGrammarResult res) {
		this.result = res;
	}

	public List<ToIgnore> getIgnored() {
		return ((List<ToIgnore>)ResultResolver.getReturnValue(result.getObject("ignored")));
	}
}
