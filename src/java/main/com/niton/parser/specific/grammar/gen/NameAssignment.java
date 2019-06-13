package com.niton.parser.specific.grammar.gen;

import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;

public class NameAssignment {
	private SubGrammerObject obj;

	public NameAssignment(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getName() {
		if(obj.getObject("name") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("name")).joinTokens();
	}
}
