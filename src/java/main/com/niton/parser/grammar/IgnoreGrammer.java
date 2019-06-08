package com.niton.parser.grammar;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokenizer.AssignedToken;
import com.niton.parser.grammar.exectors.GrammarExecutor;
import com.niton.parser.grammar.exectors.IgnoreExecutor;

/**
 * This Grammar ignores the given grammar
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class IgnoreGrammer extends Grammar {
	private String grammar;

	public IgnoreGrammer(String name2) {
		this.grammar = name2;
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
		return new IgnoreExecutor(grammar);
	}
}
