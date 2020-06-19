package com.niton.parser.specific.grammar.gen;

import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;

public class TokenSubst {
	private SubGrammerObject obj;

	public TokenSubst(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getTokenName() {
		if(obj.getObject("tokenName") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("tokenName")).joinTokens();
	}
}
