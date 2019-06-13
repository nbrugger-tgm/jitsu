package com.niton.parser.specific.grammar.gen;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;
import java.util.ArrayList;

public class GrammarFile {
	private SubGrammerObject obj;

	public GrammarFile(SubGrammerObject obj) {
		this.obj = obj;
	}

	public FileHead getHead() {
		return new FileHead((SubGrammerObject)obj.getObject("head"));
	}

	public ArrayList<Grammar> getGrammars() {
		SubGrammerObject collection =  (SubGrammerObject)obj.getObject("grammars");
		if(collection == null) {
			return null;
		}
		ArrayList<Grammar> out = new ArrayList<>();
		for (GrammarObject iter : collection.objects) {
			out.add(new Grammar((SubGrammerObject) iter));
		}
		return out;
	}
}
