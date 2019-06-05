package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Cheks if the grammer is right if yes it adds the element to the output if not
 * it is ignored
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class OptinalTokenGrammer extends Grammar {
	private String token;

	public OptinalTokenGrammer(String token, String name) {
		this.token = token;
		setName(name);
	}

	/**
	 * @see com.niton.parser.check.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {

		TokenGrammerObject tgo = new TokenGrammerObject();
		tgo.setName(getName());
		if (tokens.get(index()).name.equals(token)) {
			tgo.tokens.add(tokens.get(index()));
			increase();
			return tgo;
		} else {
			return new IgnoredGrammerObject();
		}
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
