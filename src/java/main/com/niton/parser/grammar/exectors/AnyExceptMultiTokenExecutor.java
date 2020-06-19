package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This grammer accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fullfilles
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class AnyExceptMultiTokenExecutor extends GrammarExecutor {
	
	/**
	 * @return the dunnoaccept
	 */
	public String[] getDunnoaccept() {
		return dunnoaccept;
	}

	/**
	 * @param dunnoaccept the dunnoaccept to set
	 */
	public void setDunnoaccept(String[] dunnoaccept) {
		this.dunnoaccept = dunnoaccept;
	}

	private String[] dunnoaccept;

	public AnyExceptMultiTokenExecutor(String[] string, String name) {
		this.dunnoaccept = string;
		this.setName(name);
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens, GrammarReference reference) throws ParsingException {
		TokenGrammarObject obj = new TokenGrammarObject();
		obj.setName(getName());
		for (; index() < tokens.size(); increase()) {
			AssignedToken token = tokens.get(index());
			for (String string : dunnoaccept) {
				if (token.name.equals(string))
					return obj;
			}
			
			obj.tokens.add(token);
		}
		return null;
	}
}
