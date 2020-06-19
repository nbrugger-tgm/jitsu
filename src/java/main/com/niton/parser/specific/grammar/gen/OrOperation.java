package com.niton.parser.specific.grammar.gen;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import java.lang.String;
import java.util.ArrayList;

public class OrOperation {
	private SubGrammerObject obj;

	public OrOperation(SubGrammerObject obj) {
		this.obj = obj;
	}

	public String getAnyExcept() {
		if(obj.getObject("anyExcept") == null) {
			return null;
		}
		return ((TokenGrammarObject)obj.getObject("anyExcept")).joinTokens();
	}

	public ArrayList<ArrayItem> getItems() {
		SubGrammerObject collection =  (SubGrammerObject)obj.getObject("items");
		if(collection == null) {
			return null;
		}
		ArrayList<ArrayItem> out = new ArrayList<>();
		for (GrammarObject iter : collection.objects) {
			out.add(new ArrayItem((SubGrammerObject) iter));
		}
		return out;
	}

	public NameAssignment getAssignment() {
		return new NameAssignment((SubGrammerObject)obj.getObject("assignment"));
	}
}
