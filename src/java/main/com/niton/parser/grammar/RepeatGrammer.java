package com.niton.parser.grammar;

import com.niton.parser.GrammarObject;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.grammar.exectors.GrammarExecutor;
import com.niton.parser.grammar.exectors.RepeatExecutor;

/**
 * Checks the grammar as often as is ocures
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class RepeatGrammer extends Grammar {
	private String check;

	public RepeatGrammer(String gramarReference, String name) {
		this.check = gramarReference;
		setName(name);
	}


	/**
	 * @return the check
	 */
	public String getCheck() {
		return check;
	}

	/**
	 * @param check the check to set
	 */
	public void setCheck(String check) {
		this.check = check;
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return SubGrammerObject.class;
	}


	/**
	 * @see com.niton.parser.grammar.Grammar#getExecutor()
	 */
	@Override
	public GrammarExecutor getExecutor() {
		return new RepeatExecutor(check, getName());
	}
	
}
