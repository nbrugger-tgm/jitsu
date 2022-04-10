package com.niton.parser.token;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GenericToken implements Tokenable {
	private final TokenPattern tokenPattern;
	private final String       name;
	@Override
	public String name() {
		return name;
	}

	@Override
	public String pattern() {
		return tokenPattern.getCompletePattern().pattern();
	}
}
