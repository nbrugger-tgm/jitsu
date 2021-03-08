package com.niton.parser.example.generated;

import com.niton.parser.Tokenable;
import java.lang.String;

enum CustomTokens implements Tokenable {
	LETTERS("[A-Za-z]+"),

	NUMBER("[0-9]+"),

	WHITESPACE("[ \t]+"),

	POINT("\\."),

	COMMA(","),

	PLUS("\\+"),

	MINUS("-"),

	STRING_DELIMITER("\""),

	STAR("\\*"),

	SEMICOLON(";"),

	COLON(":"),

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

	final String pattern;

	CustomTokens(String pattern) {
		this.pattern = pattern;
	}

	public String pattern() {
		return pattern;
	}
}
