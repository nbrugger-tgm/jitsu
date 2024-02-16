package com.niton.jainparse.token;

import java.util.regex.Pattern;

/**
 * Extending this interface qualifies instances as useable tokens
 *
 * @author Nils Brugger
 * @version 2019-06-09
 */
public interface Tokenable {
	/**
	 * The name of this token to be used in the tokenization process
	 */
	String name();

	/**
	 * A valid regex
	 */
	String pattern();

	default TokenPattern toTokenPattern(){
		return new TokenPattern(pattern());
	}

	default Pattern compile(){
		return Pattern.compile(pattern());
	}
}

