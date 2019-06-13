package com.niton.parser.grammar;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;
import com.niton.parser.grammar.exectors.GrammarExecutor;
import com.niton.parser.grammar.exectors.GrammarMatchExecutor;

/**
 * This is the GrammarMatchGrammer Class
 * 
 * @author Nils Brugger
 * @version 2019-06-05
 */
public class GrammarMatchGrammer extends Grammar {
	private String grammar;

	/**
	 * @return the grammar
	 */
	public String getGrammar() {
		return grammar;
	}

	/**
	 * @param grammar the grammar to set
	 */
	public void setGrammar(String grammar) {
		this.grammar = grammar;
	}


	public GrammarMatchGrammer(String grammar, String name) {
		super();
		this.grammar = grammar;
		setName(name);
	}

	
	/**
	 * @see com.niton.parser.grammar.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return GrammarObject.class;
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#getExecutor()
	 */
	@Override
	public GrammarExecutor getExecutor() {
		return new GrammarMatchExecutor(grammar, getName());
	}
}
