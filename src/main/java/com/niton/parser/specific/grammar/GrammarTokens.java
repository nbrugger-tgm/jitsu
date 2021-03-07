package com.niton.parser.specific.grammar;

import com.niton.parser.Tokenable;

/**
 * This is the GrammarTokens Class
 *
 * @author Nils Brugger
 * @version 2019-06-09
 */
enum GrammarTokens implements Tokenable {
	LINE_END("(\\r?\\n)+"),
	IDENTIFYER("[A-Za-z_]+"),
	EQ("[=]"),
	SPACE("[ \t]+"),
	SLASH("\\/"),
	COMMA(","),
	COLON(":"),
	EOF("\\Z"),

	//SOME Escaping s***
	QUOTE("(?<!\\\\)\\'"),
	ESCAPED_QUOTE("\\\\\\'"),
	BACKSLASH("\\\\(?!\\')"),


	//Specific
	TOKEN_SIGN("#"),
	OPTIONAL_SIGN("\\?"),
	REPEAT_SIGN("\\*"),
	ARROW(">"),
	ARRAY_OPEN("\\{"),
	ARRAY_CLOSE("\\}"),
	IGNORE_SIGN("~"),
	ANY_EXCEPT_SIGN("!");
	public final String regex;

	/**
	 * Creates an Instance of GrammarTokens.java
	 *
	 * @author Nils Brugger
	 * @version 2019-06-09
	 */
	GrammarTokens(String regex) {
		this.regex = regex;
	}

	@Override
	public String pattern() {
		return regex;
	}
}

