package com.niton.parser.specific.grammar.gen;

import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;

public class TokenLiteral {
	private SubGrammerObject obj;

	public TokenLiteral(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getRegex() {
		if(obj.getObject("regex") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("regex")).joinTokens();
	}
}
