package com.niton.parser.grammar;

/**
 * This is the Tokenable Class
 * @author Nils Brugger
 * @version 2019-06-09
 */
public interface Tokenable<T extends Enum & Tokenable<T>> {
	/**
	 * The name of this token to be used in the tokenization process
	 */
	public String name();

	/**
	 * A valid regex
	 */
	public String pattern();
}

