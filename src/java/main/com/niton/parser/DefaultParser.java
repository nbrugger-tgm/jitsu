package com.niton.parser;

import com.niton.parser.grammar.Grammar;

/**
 * This is the DefaultParser Class
 * @author Nils Brugger
 * @version 2019-06-12
 */
public class DefaultParser extends Parser<GrammarObject> {

	/**
	 * @see com.niton.parser.Parser#convert(com.niton.parser.GrammarObject)
	 */
	@Override
	public GrammarObject convert(GrammarObject o) {
		return o;
	}

	public DefaultParser(GrammarReference csv, Grammar root) {
		super(csv, root);
	}

	public DefaultParser(GrammarReference csv, String root) {
		super(csv, root);
	}
}

