package com.niton.parser.specific.grammar.gen;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;

public class Rule {
	private SubGrammerObject obj;

	public Rule(SubGrammerObject obj) {
		this.obj = obj;
	}

	public GrammarObject getOperation() {
		return (GrammarObject) obj.getObject("operation");
	}
}
