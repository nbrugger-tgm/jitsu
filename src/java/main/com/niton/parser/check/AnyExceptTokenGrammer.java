package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This grammer accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fullfilles
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class AnyExceptTokenGrammer extends Grammar {

	/**
	 * @return the dunnoaccept
	 */
	public String getDunnoaccept() {
		return dunnoaccept;
	}

	/**
	 * @param dunnoaccept the dunnoaccept to set
	 */
	public void setDunnoaccept(String dunnoaccept) {
		this.dunnoaccept = dunnoaccept;
	}

	private String dunnoaccept;

	public AnyExceptTokenGrammer(String string, String name) {
		this.dunnoaccept = string;
		this.setName(name);
	}

	/**
	 * @see com.niton.parser.check.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
		TokenGrammerObject obj = new TokenGrammerObject();
		obj.setName(getName());
		for (; index() < tokens.size(); increase()) {
			AssignedToken token = tokens.get(index());
			if (token.name.equals(dunnoaccept))
				return obj;
			obj.tokens.add(token);
		}
		return null;
	}

	/**
	 * @see com.niton.parser.check.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return TokenGrammerObject.class;
	}
}
