package com.niton.parser.check;

import java.util.ArrayList;
import java.util.Arrays;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Checks agains all given Grammers syncron and returns the first matching
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class MultiGrammer extends Grammar {
	private Grammar[] tokens;

	public MultiGrammer(Grammar[] tokens, String name) {
		this.tokens = tokens;
		setName(name);
	}

	/**
	 * @see com.niton.parser.check.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
		for (Grammar g : this.tokens) {
			try {
				GrammarObject obj = g.check(tokens, index());
				if (obj == null)
					throw new ParsingException("");
				index(g.index());
				obj.setName(getName());
				return obj;
			} catch (ParsingException e) {
			}
		}
		throw new ParsingException(
				"Expected Grammer (OR) : " + Arrays.toString(this.tokens) + " but none of them was parsable");
	}

	/**
	 * @return the tokens
	 */
	public Grammar[] getTokens() {
		return tokens;
	}

	/**
	 * @param tokens the tokens to set
	 */
	public void setTokens(Grammar[] tokens) {
		this.tokens = tokens;
	}

	/**
	 * @see com.niton.parser.check.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return GrammarObject.class;
	}
}
