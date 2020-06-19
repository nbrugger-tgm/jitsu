package com.niton.parser;

import java.util.ArrayList;

import com.niton.parser.Tokenizer.AssignedToken;

/**
 * The result of the {@link com.niton.parser.grammar.TokenGrammer}
 * A very raw type only containing matching tokens
 *
 * @author Nils
 * @version 2019-05-29
 */
public class TokenGrammarObject extends GrammarObject {
	public ArrayList<AssignedToken> tokens = new ArrayList<>();

	/**
	 * Joins the values of the tokens together eventualy reproducing the source 1:1
	 * @return the joined token values
	 */
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
