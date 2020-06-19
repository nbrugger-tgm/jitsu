package com.niton.parser;

import com.niton.parser.grammar.Tokenable;

/**
 * A collection of universal usable Tokens
 * 
 * @author Nils
 * @version 2019-05-28
 */
public enum Tokens implements Tokenable{
    LETTERS("[A-Za-zöäüß]+"),
    NUMBER("[0-9]+"),
    WHITESPACE(" +"),
    POINT("\\."),
    COMMA(","),
    PLUS("\\+"),
    MULTIPLICATOR("\\*"),
    STRING_DELMITTER("\""),
    SEMICOLON(";"),
    QUESTIONMARK("\\?"),
    BRACKET_OPEN("\\("),
    BRACKET_CLOSED("\\)"),
    MINUS("-"), 
    SLASH("\\/"), 
    BACK_SLASH("\\\\"),
    NEW_LINE("$");
    public final String pattern;

    Tokens(String token) {
	pattern = token;
    }

    @Override
    public String pattern() {
        return pattern;
    }
}
