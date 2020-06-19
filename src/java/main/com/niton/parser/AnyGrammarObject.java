package com.niton.parser;

/**
 * The result of a {@link com.niton.parser.grammar.MultiGrammer}
 * This is the result if it was not sure which type of grammar was going to be at this position so this is kinf of a wildcard but in its parsed form the type is known and accessible
 * @author Nils Brugger
 * @version 2019-06-14
 */
public class AnyGrammarObject extends SubGrammerObject {
	private String type;
	/**
	 * @see com.niton.parser.GrammarObject#getName()
	 */
	@Override
	public String getName() {
		return super.getName();
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
}

