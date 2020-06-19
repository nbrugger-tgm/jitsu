package com.niton.parser;

/**
 * A Grammar Object is the result after parsing a grammar. It should contain all tokens which fall under its grammar rules. It should also make "named" elements accessible
 * @author Nils
 * @version 2019-05-28
 */
public class GrammarObject {
	private String name;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * The name is used to identify the object in a group of many GrammarObjects
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
}

