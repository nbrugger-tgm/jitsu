package com.niton.parser.specific.grammar.gen;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;
import java.util.ArrayList;

public class Allignore {
	private SubGrammerObject obj;

	public Allignore(SubGrammerObject obj) {
		this.obj = obj;
	}

	public ArrayList<Combineignore> getIgnored() {
		SubGrammerObject collection =  (SubGrammerObject)obj.getObject("ignored");
		if(collection == null) {
			return null;
		}
		ArrayList<Combineignore> out = new ArrayList<>();
		for (GrammarObject iter : collection.objects) {
			out.add(new Combineignore((SubGrammerObject) iter));
		}
		return out;
	}
}
