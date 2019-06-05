package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Checks the grammar as often as is ocures
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class RepeatGrammer extends Grammar {
	private Grammar check;

	public RepeatGrammer(Grammar expression, String name) {
		this.check = expression;
		setName(name);
	}

	/**
	 * @see com.niton.parser.check.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
		boolean keep = true;
		SubGrammerObject obj = new SubGrammerObject();
		obj.setName(getName());
		while (keep) {
			try {
				GrammarObject gr = check.check(tokens, index());
				if (gr == null)
					throw new ParsingException("");
				obj.objects.add(gr);
				index(check.index());
			} catch (ParsingException e) {
				return obj;
			}
		}
		return obj;
	}

	/**
	 * @return the check
	 */
	public Grammar getCheck() {
		return check;
	}

	/**
	 * @param check the check to set
	 */
	public void setCheck(Grammar check) {
		this.check = check;
	}

	/**
	 * @see com.niton.parser.check.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return SubGrammerObject.class;
	}
	
}
