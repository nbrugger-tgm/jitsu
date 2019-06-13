package com.niton.parser.grammar;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReferenceMap;
import com.niton.parser.grammar.exectors.GrammarExecutor;

/**
 * A grammar is a pattern to check tokens against
 * 
 * @author Nils
 * @version 2019-05-28
 */
public abstract class Grammar {
	private String name;

	

	public static ChainGrammer build(String string) {
		return new ChainGrammer().name(string);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	public void map(GrammarReferenceMap ref) {
		ref.map(this);
	}
	
	public abstract Class<? extends GrammarObject> getGrammarObjectType();
	
	public abstract GrammarExecutor getExecutor();
}
