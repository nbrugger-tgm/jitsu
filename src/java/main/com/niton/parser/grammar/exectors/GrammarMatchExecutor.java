package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the GrammarMatchGrammer Class
 * 
 * @author Nils Brugger
 * @version 2019-06-05
 */
public class GrammarMatchExecutor extends GrammarExecutor {
	
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


	public GrammarMatchExecutor(String grammar, String name) {
		super();
		this.grammar = grammar;
		setName(name);
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens,GrammarReference ref) throws ParsingException {

		GrammarExecutor g = ref.get(grammar).getExecutor();
		GrammarObject o = g.check(tokens,index(),ref);
		o.setName(getName());
		index(g.index());
		return o;
	}
}
