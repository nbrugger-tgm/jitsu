package com.niton.parser.specific.grammar.gen;

import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;

public class GrammarLiteral {
	private SubGrammerObject obj;

	public GrammarLiteral(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getGrammarName() {
		if(obj.getObject("grammarName") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("grammarName")).joinTokens();
	}
}
