package com.niton.parser.grammar.exectors;

import java.util.ArrayList;
import java.util.Iterator;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Ignores the token and adds empty elements to the object
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class IgnoreTokenExecutor extends GrammarExecutor {
	private String token;

	public IgnoreTokenExecutor(String name2) {
		this.token = name2;
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens, GrammarReference ref) throws ParsingException {
		Iterator<AssignedToken> ts = tokens.listIterator(index());
		while (ts.hasNext() && ts.next().name.equals(token))
			increase();
		return new IgnoredGrammerObject();
	}

	public final GrammarObject check(ArrayList<AssignedToken> tokens, int pos, GrammarReference reference)
			throws ParsingException {
		index(pos);
		if (index() >= tokens.size())
			return new IgnoredGrammerObject();
		return process(tokens, reference);
	}

}
