package com.niton.parser;

/**
 * Extending this interface qualifies instances as useable tokens
 * @author Nils Brugger
 * @version 2019-06-09
 */
public interface Tokenable {
	/**
	 * The name of this token to be used in the tokenization process
	 */
	public String name();

	/**
	 * A valid regex
	 */
	public String pattern();
}

