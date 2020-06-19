package com.niton.example.generated;

import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;

public class Number {
	private SubGrammerObject obj;

	public Number(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getValue() {
		if(obj.getObject("value") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("value")).joinTokens();
	}
}
