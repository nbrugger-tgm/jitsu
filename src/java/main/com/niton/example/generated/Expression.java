package com.niton.example.generated;

import com.niton.parser.AnyGrammarObject;
import com.niton.parser.SubGrammerObject;

public class Expression {
	private SubGrammerObject obj;

	public Expression(SubGrammerObject obj) {
		this.obj = obj;
	}

	public AnyGrammarObject getMatched() {
		return (AnyGrammarObject) obj.getObject("matched");
	}
}
