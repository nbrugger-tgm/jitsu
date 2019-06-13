package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Checks the grammar as often as is ocures
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class RepeatExecutor extends GrammarExecutor {
	
	private String check;

	public RepeatExecutor(String expression, String name) {
		this.check = expression;
		setName(name);
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens,GrammarReference ref) throws ParsingException {
		boolean keep = true;
		SubGrammerObject obj = new SubGrammerObject();
		obj.setName(getName());
		while (keep) {
			try {
				GrammarExecutor g = ref.get(check).getExecutor();
				GrammarObject gr = g.check(tokens,index(),ref);
				if (gr == null)
					throw new ParsingException("");
				obj.objects.add(gr);
				index(g.index());
			} catch (ParsingException e) {
				return obj;
			}
		}
		return obj;
	}

	/**
	 * @return the check
	 */
	public String getCheck() {
		return check;
	}

	/**
	 * @param check the check to set
	 */
	public void setCheck(String check) {
		this.check = check;
	}

	public GrammarObject check(ArrayList<AssignedToken> tokens, int pos,GrammarReference reference) throws ParsingException {
		index(pos);
		if (index() >= tokens.size())
			return new IgnoredGrammerObject();
		return process(tokens, reference);
	}
	
}
