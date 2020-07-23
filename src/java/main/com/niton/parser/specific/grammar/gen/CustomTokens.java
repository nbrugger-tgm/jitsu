package com.niton.parser.specific.grammar.gen;

import com.niton.parser.Tokenable;
import java.lang.String;

enum CustomTokens implements Tokenable {
	LINE_END("(\\r?\\n)+"),

	IDENTIFYER("[A-Za-z_]+"),

	EQ("[=]"),

	SPACE("[ \t]+"),

	SLASH("\\/"),

	COMMA(","),

	COLON(":"),

	EOF("\\Z"),

	QUOTE("(?<!\\\\)\\'"),

	ESCAPED_QUOTE("\\\\\\'"),

	BACKSLASH("\\\\(?!\\')"),

	TOKEN_SIGN("#"),

	OPTIONAL_SIGN("\\?"),

	REPEAT_SIGN("\\*"),

	ARROW(">"),

	ARRAY_OPEN("\\{"),

	ARRAY_CLOSE("\\}"),

	IGNORE_SIGN("~"),

	ANY_EXCEPT_SIGN("!");

	final String pattern;

	CustomTokens(String pattern) {
		this.pattern = pattern;
	}

	public String pattern() {
		return pattern;
	}
}
