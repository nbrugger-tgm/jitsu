package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;

public class SubGrammar {
	private final SuperGrammarResult result;

	public SubGrammar(SuperGrammarResult res) {
		this.result = res;
	}

	public String getOperation() {
		ResultResolver.setResolveAny(true);
		return (String) ResultResolver.getReturnValue(result.getObject("operation"));
	}

	public AnyGrammarResult getItem() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("item"));
	}

	public String getRepeat() {
		if (result.getObject("repeat") == null) {
			return null;
		}
		return ((String) ResultResolver.getReturnValue(result.getObject("repeat")));
	}

	public NameAssignment getAssignment() {
		if (ResultResolver.getReturnValue(result.getObject("assignment")) == null) return null;
		return new NameAssignment((SuperGrammarResult) ResultResolver.getReturnValue(result.getObject(
				"assignment")));
	}
}
