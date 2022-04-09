package com.niton.parser.grammar.api;

import java.util.Set;

/**
 * Used to resolve grammar names
 *
 * @author Nils Brugger
 * @version 2019-06-07
 */
public interface GrammarReference {
	/**
	 * Should return the grammar associated with the key
	 *
	 * @param key the name of the grammar
	 * @return the grammar
	 */
	Grammar<?,?> get(String key);

	/**
	 * @return the name of all contained Grammars
	 */
	Set<String> grammarNames();

}

