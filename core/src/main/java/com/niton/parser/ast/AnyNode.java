package com.niton.parser.ast;

import com.niton.parser.grammar.types.MultiGrammar;
import com.niton.parser.token.Tokenizer;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

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

	public AnyNode(AstNode res) {
		this.res = res;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return res.getOriginGrammarName();
	}

	@Override
	public Collection<Tokenizer.AssignedToken> join() {
		return res.join();
	}

	@Override
	public ReducedNode reduce(@NonNull String name) {
		var innerNode = res.reduce("value");
		if(innerNode == null)
			return null;
		if(getType() != null) {
			return ReducedNode.node(name, List.of(
					ReducedNode.leaf("type", getType()),
					innerNode
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

