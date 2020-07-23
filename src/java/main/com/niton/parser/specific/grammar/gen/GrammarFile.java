package com.niton.parser.specific.grammar.gen;

import com.niton.parser.ResultResolver;
import com.niton.parser.result.SuperGrammarResult;
import java.util.List;

public class GrammarFile {
	private SuperGrammarResult result;

	public GrammarFile(SuperGrammarResult res) {
		this.result = res;
	}

	public FileHead getHead() {
		return new FileHead(result.getObject("head"));
	}

	public List<RootGrammar> getGrammars() {
		return ((List<RootGrammar>)ResultResolver.getReturnValue(result.getObject("grammars")));
	}
}
