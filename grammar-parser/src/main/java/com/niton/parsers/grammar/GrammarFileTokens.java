package com.niton.parsers.grammar;

import com.niton.parser.token.Tokenable;

/**
 * This is the GrammarTokens Class
 *
 * @author Nils Brugger
 * @version 2019-06-09
 */
public enum GrammarFileTokens implements Tokenable {
	LINE_END("(\\r?\\n)+"),
	IDENTIFIER("[A-Za-z_]+"),
	EQ("[=]"),
	SPACE("[ \t]+"),
	SLASH("\\/"),
	COMMA(","),
	COLON(":"),
	EOF("\\Z(?!\\r?\\n)"),

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
	 */
	GrammarFileTokens(String regex) {
		this.regex = regex;
	}

	@Override
	public String pattern() {
		return regex;
	}
}

