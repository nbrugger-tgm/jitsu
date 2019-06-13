package com.niton.parser.grammar;

import java.util.ArrayList;
import java.util.Iterator;

import com.niton.parser.GrammarObject;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;
import com.niton.parser.grammar.exectors.GrammarExecutor;
import com.niton.parser.grammar.exectors.IgnoreTokenExecutor;

/**
 * Ignores the token and adds empty elements to the object
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class IgnoreTokenGrammer extends Grammar {
	private String token;

	public IgnoreTokenGrammer(String name2) {
		this.token = name2;
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return IgnoredGrammerObject.class;
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#getExecutor()
	 */
	@Override
	public GrammarExecutor getExecutor() {
		return new IgnoreTokenExecutor(token);
	}

}
