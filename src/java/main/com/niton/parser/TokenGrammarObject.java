package com.niton.parser;

import java.util.ArrayList;

import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the TokenGrammerObject Class
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class TokenGrammarObject extends GrammarObject {
	public ArrayList<AssignedToken> tokens = new ArrayList<>();

	public String joinTokens() {
		StringBuilder builder = new StringBuilder();
		for (AssignedToken grammerObject : tokens) {
			builder.append(grammerObject.value);
		}
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (AssignedToken t : tokens) {
			builder.append("\n\t");
			builder.append(t.toString());
		}
		builder.append("\n]");
		return builder.toString();
	}
}
