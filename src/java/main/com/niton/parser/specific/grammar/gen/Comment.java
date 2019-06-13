package com.niton.parser.specific.grammar.gen;

import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;

public class Comment {
	private SubGrammerObject obj;

	public Comment(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getMessage() {
		if(obj.getObject("message") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("message")).joinTokens();
	}
}
