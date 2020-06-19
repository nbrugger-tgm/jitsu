package com.niton.parser.specific.grammar;

import com.niton.parser.grammar.Tokenable;

/**
 * This is the GrammarTokens Class
 * @author Nils Brugger
 * @version 2019-06-09
 */
public enum GrammarTokens implements Tokenable{
	LINE_END("[\\r\\n]+"),
	IDENTIFYER("[A-Za-z_]+"),
	EQ("[=]"),
	WHITESPACE("[ \t]+"),
	SLASH("\\/"),
	COMMA(","),
	COLON(":"),
	EOF("\\Z"),

	//SOME Escaping shit
	QUOTE("(?<!\\\\)\\'"),
	ESCAPED_QUOTE("\\\\\\'"),
	BACKSLASH("\\\\(?!\\')"),



	//Specific
	TOKEN_SIGN("#"),
	OPTIONAL("\\?"),
	STAR("\\*"),
	ARROW(">"),
	ARRAY_OPEN("\\{"),
	ARRAY_CLOSE("\\}"),
	IGNORE("~"),
	NEGATE("!");
	public final String regex;

	/**
	 * Creates an Instance of GrammarTokens.java
	 * @author Nils Brugger
	 * @version 2019-06-09
	 */
	private GrammarTokens(String regex) {
		this.regex = regex;
	}

	@Override
	public String pattern() {
		return regex;
	}
}

