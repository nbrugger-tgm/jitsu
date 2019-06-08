package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the RepeatGrammer Class
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class RepeatTokenExecutor extends GrammarExecutor {
	private String token;
	

	public RepeatTokenExecutor(String token, String name) {
		this.token = token;
		this.setName(name);
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens,GrammarReference ref) throws ParsingException {
		TokenGrammarObject object = new TokenGrammarObject();
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
}
