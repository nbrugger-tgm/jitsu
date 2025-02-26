package com.niton.jainparse.token;

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
	DOLLAR("\\$"),
	DOT("\\."),
	COMMA(","),
	PLUS("\\+"),
	MINUS("-"),

	STAR("\\*"),
	SEMICOLON(";"),
	COLON(":"),
	PIPE("\\|"),
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


	LEFT_ANGLE_BRACKET("<"),
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
