package com.niton.parser.specific.grammar.gen;

import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;

public class TokenDefiner {
	private SubGrammerObject obj;

	public TokenDefiner(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getName() {
		if(obj.getObject("name") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("name")).joinTokens();
	}

	public TokenLiteral getLiteral() {
		return new TokenLiteral((SubGrammerObject)obj.getObject("literal"));
	}
}
