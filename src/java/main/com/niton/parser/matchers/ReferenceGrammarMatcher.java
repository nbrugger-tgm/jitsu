package com.niton.parser.matchers;

import com.niton.parser.GrammarMatcher;
import com.niton.parser.token.TokenStream;
import com.niton.parser.Grammar;
import com.niton.parser.GrammarResult;
import com.niton.parser.GrammarReference;
import com.niton.parser.exceptions.ParsingException;

/**
 * This is the GrammarMatchGrammar Class
 * 
 * @author Nils Brugger
 * @version 2019-06-05
 */
public class ReferenceGrammarMatcher extends GrammarMatcher<GrammarResult> {
	
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


	public ReferenceGrammarMatcher(String grammar) {
		super();
		this.grammar = grammar;
	}
	@Override
	protected GrammarResult process(TokenStream tokens, GrammarReference ref) throws ParsingException {
		if(ref.get(grammar) == null)
			throw new ParsingException("Unknown reference! The Grammar \""+grammar+"\" needed in "+getOriginGrammarName()+" was not found in reference");
		Grammar g = ref.get(grammar);
		try {
			GrammarResult o = g.parse(tokens, ref);
			tokens.commit();
			return o;
		}catch (ParsingException e){
			tokens.rollback();
			throw e;
		}
	}
}
