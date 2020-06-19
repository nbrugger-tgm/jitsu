package com.niton.parser;

import java.util.Collection;
import java.util.Set;

import com.niton.parser.grammar.Grammar;

/**
 * Used to resolve grammar names
 * @author Nils Brugger
 * @version 2019-06-07
 */
public interface GrammarReference {
	/**
	 * Should return the grammar associated with the key
	 * @param key the name of the grammar
	 * @return the grammar
	 */
	public Grammar get(String key);

	/**
	 * @return the name of all contained Grammars
	 */
	public Set<String> grammarNames();

}

