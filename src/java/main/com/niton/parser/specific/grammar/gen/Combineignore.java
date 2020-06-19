package com.niton.parser.specific.grammar.gen;

import com.niton.parser.AnyGrammarObject;
import com.niton.parser.SubGrammerObject;

public class Combineignore {
	private SubGrammerObject obj;

	public Combineignore(SubGrammerObject obj) {
		this.obj = obj;
	}

	public AnyGrammarObject getIgnore() {
		return (AnyGrammarObject) obj.getObject("ignore");
	}
}
