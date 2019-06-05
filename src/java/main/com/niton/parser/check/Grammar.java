package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * A grammar is a pattern to check tokens against
 * 
 * @author Nils
 * @version 2019-05-28
 */
public abstract class Grammar {
	private int index;
	private String name;

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 * @throws ParsingException
	 */
	public final GrammarObject check(ArrayList<AssignedToken> tokens) throws ParsingException {
		index = 0;
		return process(tokens);
	}

	public static ChainGrammer build(String string) {
		return new ChainGrammer().name(string);
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
	public final GrammarObject check(ArrayList<AssignedToken> tokens, int pos) throws ParsingException {
		if (index >= tokens.size())
			throw new ParsingException("No More Tokens!");
		index = pos;
		return process(tokens);
	}

	public abstract GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException;

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
	
	public abstract Class<? extends GrammarObject> getGrammarObjectType();
}
