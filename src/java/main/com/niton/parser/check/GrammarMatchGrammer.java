package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the GrammarMatchGrammer Class
 * 
 * @author Nils Brugger
 * @version 2019-06-05
 */
public class GrammarMatchGrammer extends Grammar {
	private Grammar grammar;

	/**
	 * @return the grammar
	 */
	public Grammar getGrammar() {
		return grammar;
	}

	/**
	 * @param grammar the grammar to set
	 */
	public void setGrammar(Grammar grammar) {
		this.grammar = grammar;
	}


	public GrammarMatchGrammer(Grammar grammar, String name) {
		super();
		this.grammar = grammar;
		setName(name);
	}

	/**
	 * @see com.niton.parser.check.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
		GrammarObject o = grammar.check(tokens,index());
		o.setName(getName());
		index(grammar.index());
		return o;
	}

	/**
	 * @see com.niton.parser.check.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return GrammarObject.class;
	}
}
