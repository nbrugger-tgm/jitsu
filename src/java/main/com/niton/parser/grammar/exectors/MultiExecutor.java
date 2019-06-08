package com.niton.parser.grammar.exectors;

import java.util.ArrayList;
import java.util.Arrays;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Checks agains all given Grammers syncron and returns the first matching
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class MultiExecutor extends GrammarExecutor {
	
	private String[] tokens;

	public MultiExecutor(String[] tokens, String name) {
		this.tokens = tokens;
		setName(name);
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens,GrammarReference ref) throws ParsingException {
		for (String grammar : this.tokens) {
			try {
				GrammarExecutor g = ref.get(grammar).getExecutor();
				GrammarObject obj = g.check(tokens,index(),ref);
				if (obj == null)
					throw new ParsingException("");
				index(g.index());
				obj.setName(getName());
				return obj;
			} catch (ParsingException e) {
			}
		}
		throw new ParsingException(
				"Expected Grammer (OR) : " + Arrays.toString(this.tokens) + " but none of them was parsable");
	}

	/**
	 * @return the tokens
	 */
	public String[] getTokens() {
		return tokens;
	}

	/**
	 * @param tokens the tokens to set
	 */
	public void setTokens(String[] tokens) {
		this.tokens = tokens;
	}


}
