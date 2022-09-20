package com.niton.parser.token;

/**
 * A collection of universal usable Tokens
 *
 * @author Nils
 * @version 2019-05-28
 */
public enum DefaultToken implements Tokenable {
	LETTERS("[A-Za-z]+"),
	NUMBER("[0-9]+"),
	WHITESPACE("[ \t]+"),

	POINT("\\."),
	COMMA(","),
	PLUS("\\+"),
	MINUS("-"),

	STAR("\\*"),
	SEMICOLON(";"),
	COLON(":"),
	QUOTE("'"),
	DOUBLEQUOTE("\""),
	QUESTIONMARK("\\?"),
	UNDERSCORE("_"),
	EQUAL("="),

	BRACKET_OPEN("\\("),
	BRACKET_CLOSED("\\)"),

	ROUND_BRACKET_OPEN("\\{"),
	ROUND_BRACKET_CLOSED("\\}"),

	SQUARE_BRACKET_OPEN("\\["),
	SQUARE_BRACKET_CLOSED("\\]"),


	SMALLER("<"),
	BIGGER(">"),

	SLASH("\\/"),
	BACK_SLASH("\\\\"),
	NEW_LINE("\\r?\\n"),
	EOF("\\Z(?!\\r?\\n)");
	public final String regex;

	DefaultToken(String token) {
		regex = token;
	}

	@Override
	public String pattern() {
		return regex;
	}
}
