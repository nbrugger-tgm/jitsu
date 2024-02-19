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
	 * A valid regex
	 */
	String pattern();

	default Pattern compile(){
		return Pattern.compile(pattern());
	}
}

