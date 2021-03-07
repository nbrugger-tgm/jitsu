package com.niton.parser.exceptions;

/**
 * An exception that happens during parsing
 *
 * @author Nils
 * @version 2019-05-29
 */
public class ParsingException extends Exception {
	/**
	 * Type: long
	 * Name: ParsingException.java
	 * Description:
	 */
	private static final long serialVersionUID = -894052289785347038L;

	/**
	 * Creates an Instance of ParsingException.java
	 *
	 * @param message
	 * @author Nils
	 * @version 2019-05-29
	 */
	public ParsingException(String message) {
		super(message);
	}

	public ParsingException(String message, ParsingException lastException) {
		super(message, lastException);
	}
}

