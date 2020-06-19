package com.niton.parser.specific.grammar.gen;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;
import java.util.ArrayList;

public class FileHead {
	private SubGrammerObject obj;

	public FileHead(SubGrammerObject obj) {
		this.obj = obj;
	}

	public ArrayList<IgnoringTokenDefiner> getTokenDefiners() {
		SubGrammerObject collection =  (SubGrammerObject)obj.getObject("tokenDefiners");
		if(collection == null) {
			return null;
		}
		ArrayList<IgnoringTokenDefiner> out = new ArrayList<>();
		for (GrammarObject iter : collection.objects) {
			out.add(new IgnoringTokenDefiner((SubGrammerObject) iter));
		}
		return out;
	}
}
