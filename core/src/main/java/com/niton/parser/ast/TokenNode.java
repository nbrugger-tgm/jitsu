package com.niton.parser.ast;

import com.niton.parser.grammar.types.TokenGrammar;
import com.niton.parser.token.Tokenizer.AssignedToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The result of the {@link TokenGrammar}
 * A very raw type only containing matching tokens
 *
 * @author Nils
 * @version 2019-05-29
 */
public class TokenNode extends AstNode {
	public List<AssignedToken> tokens = new ArrayList<>();

	/**
	 * Joins the values of the tokens together eventualy reproducing the source 1:1
	 *
	 * @return the joined token values
	 */
	public String joinTokens() {
		StringBuilder builder = new StringBuilder();
		for (AssignedToken grammarObject : tokens) {
			builder.append(grammarObject.getValue());
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

	@Override
	public Collection<AssignedToken> join() {
		return tokens;
	}

	@Override
	public ReducedNode reduce(String name) {
		return ReducedNode.leaf(name, joinTokens());
	}
}
