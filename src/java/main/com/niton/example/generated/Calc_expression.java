package com.niton.example.generated;

import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;

public class Calc_expression {
	private SubGrammerObject obj;

	public Calc_expression(SubGrammerObject obj) {
		this.obj = obj;
	}

	public Expression getFirstExpression() {
		return new Expression((SubGrammerObject)obj.getObject("firstExpression"));
	}

	public String getCalculationType() {
		if(obj.getObject("calculationType") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("calculationType")).joinTokens();
	}

	public Expression getSecondExpression() {
		return new Expression((SubGrammerObject)obj.getObject("secondExpression"));
	}
}
