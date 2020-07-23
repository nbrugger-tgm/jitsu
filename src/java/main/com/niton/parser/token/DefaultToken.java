package com.niton.parser.token;

import com.niton.parser.Tokenable;

/**
 * A collection of universal usable Tokens
 * 
 * @author Nils
 * @version 2019-05-28
 */
public enum DefaultToken implements Tokenable{
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
    public final String pattern;

    DefaultToken(String token) {
	pattern = token;
    }

    @Override
    public String pattern() {
        return pattern;
    }
}
