package com.niton.parser;

import com.niton.parser.grammars.ChainGrammar;
import com.niton.parser.specific.grammar.GrammarFileContent;
import com.niton.parser.token.Tokenizer;

/**
 * The degault implementation of the parser returning the plain {@link GrammarResult}
 *
 * @author Nils Brugger
 * @version 2019-06-12
 */
public class DefaultParser extends Parser<GrammarResult> {

	public DefaultParser(GrammarReference reference, Grammar root) {
		super(reference, root);
	}

	public DefaultParser(ChainGrammar root) {
		super(root, root);
	}

	public DefaultParser(GrammarReference ref, String root) {
		super(ref, root);
	}

	public DefaultParser(GrammarFileContent gfc, String jsonValue) {
		super(gfc, jsonValue);
	}


	/**
	 * @see com.niton.parser.Parser#convert(GrammarResult)
	 */
	@Override
	public GrammarResult convert(GrammarResult o) {
		return o;
	}
}

