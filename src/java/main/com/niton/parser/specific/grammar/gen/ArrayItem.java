package com.niton.parser.specific.grammar.gen;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;

public class ArrayItem {
	private SubGrammerObject obj;

	public ArrayItem(SubGrammerObject obj) {
		this.obj = obj;
	}

	public GrammarObject getItem() {
		return (GrammarObject) obj.getObject("item");
	}
}
