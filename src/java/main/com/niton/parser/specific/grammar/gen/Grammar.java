package com.niton.parser.specific.grammar.gen;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;
import java.util.ArrayList;

public class Grammar {
	private SubGrammerObject obj;

	public Grammar(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getName() {
		if(obj.getObject("name") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("name")).joinTokens();
	}

	public ArrayList<Rule> getChain() {
		SubGrammerObject collection =  (SubGrammerObject)obj.getObject("chain");
		if(collection == null) {
			return null;
		}
		ArrayList<Rule> out = new ArrayList<>();
		for (GrammarObject iter : collection.objects) {
			out.add(new Rule((SubGrammerObject) iter));
		}
		return out;
	}
}
