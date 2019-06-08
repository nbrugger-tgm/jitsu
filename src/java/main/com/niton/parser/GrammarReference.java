package com.niton.parser;

import com.niton.parser.grammar.Grammar;

/**
 * This is the GrammarReference Class
 * @author Nils Brugger
 * @version 2019-06-07
 */
public interface GrammarReference {
	public Grammar get(String key);
}

