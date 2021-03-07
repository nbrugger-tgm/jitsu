package com.niton.parser;

import com.niton.parser.token.Tokenizer;

import java.util.Collection;

/**
 * A Grammar Object is the result after parsing a grammar. It should contain all tokens which fall under its grammar rules. It should also make "named" elements accessible
 * @author Nils
 * @version 2019-05-28
 */
public abstract class GrammarResult {
	private String originGrammarName;
	/**
	 * Collects all Tokens of underlying Grammars recursively. This leads to the original parsed text except of ignored tokens
	 * @return the ordered list of all recursive tokens
	 */
	public abstract Collection<? extends Tokenizer.AssignedToken> join() ;
	/**
	 * Simmilar to  {@link #join()} but joining the token values to a string
	 * @return
	 */
	public String joinTokens() {
		StringBuilder builder = new StringBuilder();
		for (Tokenizer.AssignedToken grammarObject : join()) {
			builder.append(grammarObject.value);
		}
		return builder.toString();
	}

	public String getOriginGrammarName() {
		return originGrammarName;
	}

	public void setOriginGrammarName(String originGrammarName) {
		this.originGrammarName = originGrammarName;
	}
}

