package com.niton.test;

import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.check.ChainGrammer;

public class BracketValue {
	private SubGrammerObject obj;

	BracketValue(ChainGrammer obj) {
		this.obj = obj;
	}

	public TokenGrammerObject getUselessBrackets() {
		return (TokenGrammerObject) obj.getObject("uselessBrackets");;
	}

	public Value getContent() {
		return new Content((ChainGrammer)obj.getObject("content"));;
	}
}
