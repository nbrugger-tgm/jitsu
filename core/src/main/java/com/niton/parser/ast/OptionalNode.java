package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

/**
 * This is the IngoredGrammarObject Class
 *
 * @author Nils
 * @version 2019-05-29
 */
public class OptionalNode extends AstNode {
	private AstNode value = null;

	/**
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return "[optional]";
	}

	public AstNode getValue() {
		return value;
	}

	public void setValue(AstNode value) {
		this.value = value;
	}

	@Override
	public Collection<Tokenizer.AssignedToken> join() {
		return value != null ? value.join() : new LinkedList<>();
	}

	@Override
	public ReducedNode reduce(String name) {
		return Optional.ofNullable(value).map(v -> v.reduce(name)).orElse(null);
	}
}

