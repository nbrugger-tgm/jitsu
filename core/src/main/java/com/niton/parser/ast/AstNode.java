package com.niton.parser.ast;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Tokenizer;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * A Grammar Object is the result after parsing a grammar. It should contain all tokens which fall
 * under its grammar rules. It should also make "named" elements accessible
 *
 * @author Nils
 * @version 2019-05-28
 */
@Getter
@Setter
public abstract class AstNode {
	private String           originGrammarName;
	private ParsingException parsingExceptions;

	/**
	 * Simmilar to  {@link #join()} but joining the token values to a string
	 *
	 * @return
	 */
	public String joinTokens() {
		StringBuilder builder = new StringBuilder();
		for (Tokenizer.AssignedToken grammarObject : join()) {
			builder.append(grammarObject.getValue());
		}
		return builder.toString();
	}

	/**
	 * Collects all Tokens of underlying Grammars recursively. This leads to the original parsed
	 * text except of ignored tokens
	 *
	 * @return the ordered list of all recursive tokens
	 */
	public abstract Collection<Tokenizer.AssignedToken> join();

	public abstract ReducedNode reduce(String name);
}

