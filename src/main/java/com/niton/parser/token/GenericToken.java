package com.niton.parser.token;

import com.niton.parser.Token;
import com.niton.parser.Tokenable;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GenericToken implements Tokenable {
	private final Token  token;
	private final String name;
	@Override
	public String name() {
		return name;
	}

	@Override
	public String pattern() {
		return token.getCompletePattern().pattern();
	}
}
