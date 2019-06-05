package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the RepeatGrammer Class
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class RepeatTokenGrammer extends Grammar {
	private String token;

	public RepeatTokenGrammer(String token, String name) {
		this.token = token;
		this.setName(name);
	}

	/**
	 * @see com.niton.parser.check.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
		TokenGrammerObject object = new TokenGrammerObject();
		object.setName(getName());
		for (int i = index(); i < tokens.size(); i++) {
			AssignedToken aToken = tokens.get(i);
			if(aToken.name.equals(token))
				object.tokens.add(aToken);
			else
				return object;
			increase();
		}
		return object;
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * @see com.niton.parser.check.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return TokenGrammerObject.class;
	}
}
