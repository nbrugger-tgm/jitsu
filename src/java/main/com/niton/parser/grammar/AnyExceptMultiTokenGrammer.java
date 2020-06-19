package com.niton.parser.grammar;

import com.niton.parser.GrammarObject;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.grammar.exectors.AnyExceptMultiTokenExecutor;
import com.niton.parser.grammar.exectors.GrammarExecutor;

/**
 * This grammer accepts any token except the one given in the Constructor<br>
 * If this token is reached the grammar is fullfilles
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class AnyExceptMultiTokenGrammer extends Grammar {

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

	public AnyExceptMultiTokenGrammer(String[] tokensNotToAccept, String name) {
		this.dunnoaccept = tokensNotToAccept;
		this.setName(name);
	}

	

	/**
	 * @see com.niton.parser.grammar.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return TokenGrammarObject.class;
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#getExecutor()
	 */
	@Override
	public GrammarExecutor getExecutor() {
		return new AnyExceptMultiTokenExecutor(dunnoaccept, getName());
	}
}
