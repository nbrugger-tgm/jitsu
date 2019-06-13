package com.niton.parser.specific.grammar.gen;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;

public class Combineignore {
	private SubGrammerObject obj;

	public Combineignore(SubGrammerObject obj) {
		this.obj = obj;
	}

	public GrammarObject getIgnore() {
		return (GrammarObject) obj.getObject("ignore");
	}
}
