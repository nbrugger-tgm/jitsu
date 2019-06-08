package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This Grammar ignores the given grammar
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class IgnoreExecutor extends GrammarExecutor {
	private String grammar;

	public IgnoreExecutor(String name2) {
		this.grammar = name2;
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens,GrammarReference ref) throws ParsingException {
		try {
			GrammarExecutor g = ref.get(grammar).getExecutor();
			g.check(tokens,index(),ref);
			index(g.index());
		} catch (ParsingException e) {
		}
		return new IgnoredGrammerObject();
	}
}
