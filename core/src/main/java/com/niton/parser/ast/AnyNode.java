package com.niton.parser.ast;

import com.niton.parser.grammar.types.MultiGrammar;
import com.niton.parser.token.Tokenizer;

import java.util.Collection;
import java.util.List;

/**
 * The result of a {@link MultiGrammar}
 * This is the result if it was not sure which type of grammar was going to be at this position so this is kind of a wildcard but in its parsed form the type is known and accessible
 *
 * @author Nils Brugger
 * @version 2019-06-14
 */
public class AnyNode extends AstNode {
	private final AstNode res;
	private       String  type;

	public AnyNode(AstNode res) {
		this.res = res;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public Collection<Tokenizer.AssignedToken> join() {
		return res.join();
	}

	@Override
	public ReducedNode reduce(String name) {
		if(type != null) {
			return ReducedNode.node(name, List.of(
					ReducedNode.leaf("type", type),
					res.reduce("value")
			));
		}else{
			return res.reduce(name);
		}
	}

	public AstNode getRes() {
		return res;
	}

	@Override
	public String toString() {
		return res.toString();
	}
}

