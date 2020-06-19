package com.niton.parser.specific.grammar.gen;

import com.niton.parser.AnyGrammarObject;
import com.niton.parser.SubGrammerObject;

public class ArrayItem {
	private SubGrammerObject obj;

	public ArrayItem(SubGrammerObject obj) {
		this.obj = obj;
	}

	public AnyGrammarObject getItem() {
		return (AnyGrammarObject) obj.getObject("item");
	}
}
