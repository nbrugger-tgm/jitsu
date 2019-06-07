package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the TokenGrammer Class
 * 
 * @author Nils
 * @version 2019-05-28
 */
public class TokenGrammer extends Grammar {
	private String tokenName;

	/**
	 * Creates an Instance of TokenGrammer.java
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 * @param tokenName
	 * @param name
	 */
	public TokenGrammer(String tokenName, String name) {
		super();
		this.tokenName = tokenName;
		setName(name);
	}

	/**
	 * @throws ParsingException
	 * @see com.niton.parser.check.Grammar#check(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
		AssignedToken token = tokens.get(index());
		if (token.name.equals(tokenName)) {
			TokenGrammarObject obj = new TokenGrammarObject();
			obj.tokens.add(token);
			obj.setName(getName());
			increase();
			return obj;
		}
		throw new ParsingException("Expected Token \"" + tokenName + "\" but actual value was  \"" + token.name+"\" (index : "+index()+") -> "+"["+(index() > 0 ? tokens.get(index()-1).value : "")+tokens.get(index()).value+(tokens.size()-index() > 1 ? tokens.get(index()+1).value : "")+"]");
	}

	/**
	 * @return the tokenName
	 */
	public String getTokenName() {
		return tokenName;
	}

	/**
	 * @param tokenName the tokenName to set
	 */
	public void setTokenName(String tokenName) {
		this.tokenName = tokenName;
	}

	/**
	 * @see com.niton.parser.check.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return TokenGrammarObject.class;
	}
}
