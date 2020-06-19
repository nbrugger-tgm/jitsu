package com.niton.parser.specific.grammar.gen;

import com.niton.parser.AnyGrammarObject;
import com.niton.parser.SubGrammerObject;

public class Rule {
	private SubGrammerObject obj;

	public Rule(SubGrammerObject obj) {
		this.obj = obj;
	}

	public AnyGrammarObject getOperation() {
		return (AnyGrammarObject) obj.getObject("operation");
	}
}
