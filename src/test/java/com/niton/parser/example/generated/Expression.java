package com.niton.parser.example.generated;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.result.SuperGrammarResult;
import java.lang.String;
import java.util.List;
import java.util.stream.Collectors;

public class Expression {
	private final SuperGrammarResult result;

	public Expression(SuperGrammarResult res) {
		this.result = res;
	}

	public AnyGrammarResult getStart() {
		ResultResolver.setResolveAny(false);
		return (AnyGrammarResult) ResultResolver.getReturnValue(result.getObject("start"));
	}

	public List<Addition> getOperands() {
		return (List<Addition>)((List<SuperGrammarResult>)ResultResolver.getReturnValue(result.getObject("operands"))).stream().map(res -> new Addition(res)).collect(Collectors.toList());
	}

	public String toString() {
		return result.joinTokens();
	}
}
