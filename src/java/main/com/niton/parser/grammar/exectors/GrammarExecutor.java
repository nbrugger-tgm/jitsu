package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the GrammarExecutor Class
 * @author Nils Brugger
 * @version 2019-06-07
 */
public abstract class GrammarExecutor {
	private int index;
	private String name;
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 * @throws ParsingException
	 */
	public final GrammarObject check(ArrayList<AssignedToken> tokens,GrammarReference reference) throws ParsingException {
		index = 0;
		return process(tokens, reference);
	}
	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param tokens
	 * @param pos
	 * @return
	 * @throws ParsingException
	 */
	public final GrammarObject check(ArrayList<AssignedToken> tokens, int pos,GrammarReference reference) throws ParsingException {
		index = pos;
		if (index >= tokens.size())
			throw new ParsingException("No More Tokens!");
		return process(tokens, reference);
	}

	protected final void increase() {
		index++;
	}

	/**
	 * @return the index
	 */
	public int index() {
		return index;
	}

	/**
	 * @return the index
	 */
	public void index(int index) {
		this.index = index;
	}

	protected abstract GrammarObject process(ArrayList<AssignedToken> tokens,GrammarReference reference) throws ParsingException;
}

