package com.niton.parser.check;

import java.util.ArrayList;
import java.util.Arrays;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Expects one of the tokens given
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class MultiTokenGrammer extends Grammar {
	private String[] tokens;

	public MultiTokenGrammer(String[] tokens, String name) {
		this.tokens = tokens;
		setName(name);
	}

	/**
	 * @see com.niton.parser.check.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
		AssignedToken token = tokens.get(index());
		for (int i = 0; i < this.tokens.length; i++) {
			if (token.name.equals(this.tokens[i])) {
				TokenGrammerObject obj = new TokenGrammerObject();
				obj.setName(getName());
				obj.tokens.add(token);
				increase();
				return obj;
			}
		}
		throw new ParsingException(
				"Expected Tokens (OR) : " + Arrays.toString(this.tokens) + " but actual value was : " + token.name);
	}

	/**
	 * @return the tokens
	 */
	public String[] getTokens() {
		return tokens;
	}

	/**
	 * @param tokens the tokens to set
	 */
	public void setTokens(String[] tokens) {
		this.tokens = tokens;
	}

	/**
	 * @see com.niton.parser.check.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return TokenGrammerObject.class;
	}
	
}
