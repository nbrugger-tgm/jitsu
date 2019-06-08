package com.niton.generated;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;

public class Expression {
	private SubGrammerObject obj;

	public Expression(SubGrammerObject obj) {
		this.obj = obj;
	}

	public GrammarObject getMatched() {
		return (GrammarObject) obj.getObject("matched");
	}
}
