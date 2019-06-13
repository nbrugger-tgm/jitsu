package com.niton.parser.grammar;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;
import com.niton.parser.grammar.exectors.GrammarExecutor;
import com.niton.parser.grammar.exectors.RepeatTokenExecutor;

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
	 * @see com.niton.parser.grammar.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return TokenGrammarObject.class;
	}


	/**
	 * @see com.niton.parser.grammar.Grammar#getExecutor()
	 */
	@Override
	public GrammarExecutor getExecutor() {
		return new RepeatTokenExecutor(token, getName());
	}
}
