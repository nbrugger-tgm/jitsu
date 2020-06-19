package com.niton.parser.specific.grammar.gen;

import com.niton.parser.SubGrammerObject;

public class IgnoringTokenDefiner {
	private SubGrammerObject obj;

	public IgnoringTokenDefiner(SubGrammerObject obj) {
		this.obj = obj;
	}

	public TokenDefiner getDefiner() {
		return new TokenDefiner((SubGrammerObject)obj.getObject("definer"));
	}
}
