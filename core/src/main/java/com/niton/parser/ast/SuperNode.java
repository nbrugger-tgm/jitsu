package com.niton.parser.ast;

import com.niton.parser.grammar.types.ChainGrammar;
import com.niton.parser.token.Tokenizer.AssignedToken;

import java.util.*;

/**
 * The result of a {@link ChainGrammar}
 * <p>
 * Describes a row of GrammarObjects. Could also be seen as Result Container.
 * This is the result of any Grammar matching more than one thing. Very important to build a syntax tree
 *
 * @author Nils
 * @version 2019-05-29
 */
public class SuperNode extends AstNode {
	public List<AstNode>        subNodes = new ArrayList<>();
	public Map<String, Integer> naming   = new LinkedHashMap<>();

	public void setNaming(Map<String, Integer> naming) {
		this.naming = naming;
	}

	public void setSubNodes(List<AstNode> subNodes) {
		this.subNodes = subNodes;
	}

	public void name(String name, AstNode res) {
		add(res);
		naming.put(name, subNodes.size() - 1);
	}

	public boolean add(AstNode grammarResult) {
		return subNodes.add(grammarResult);
	}

	public List<AssignedToken> join() {
		List<AssignedToken> token = new LinkedList<>();
		for (AstNode object : subNodes) {
			token.addAll(object.join());
		}
		return token;
	}

	@Override
	public ReducedNode reduce(String name) {
		List<ReducedNode> reduced = new ArrayList<>();
		for (var key : naming.keySet()) {
			reduced.add(getNode(key).reduce(key));
		}
		return ReducedNode.node(name, reduced);
	}


	/**
	 * Get a named sub-object by its name
	 *
	 * @param name the name of the sub object to get
	 * @return the GrammarObject
	 */
	public <T extends AstNode> T getNode(String name) {
		return (T) subNodes.get(naming.get(name));
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(getOriginGrammarName());
		for (AstNode grammarObject : subNodes) {
			builder.append("\n   ");
			builder.append(grammarObject.toString().replaceAll("\n", "\n   "));
		}
		builder.append("\n]");
		return builder.toString();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString(int depth) {
		if (depth == 0) {
			return "[" + joinTokens() + "]";
		}
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(getOriginGrammarName());
		for (AstNode grammarObject : subNodes) {
			builder.append("\n   ");
			if (grammarObject instanceof SuperNode) {
				builder.append(((SuperNode) grammarObject).toString(depth - 1)
				                                          .replaceAll("\n", "\n   "));
			} else {
				builder.append(grammarObject.toString().replaceAll("\n", "\n    "));
			}
		}
		builder.append("\n]");
		return builder.toString();
	}

	/**
	 * Description :
	 *
	 * @author Nils
	 * @version 2019-05-30
	 */
	public String toClearString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(getOriginGrammarName());
		for (AstNode grammarObject : subNodes) {
			builder.append("\n   ");

			if (grammarObject instanceof SuperNode) {
				builder.append(((SuperNode) grammarObject).toClearString()
				                                          .replaceAll("\n", "\n   "));
			} else if (grammarObject instanceof TokenNode) {
				builder.append(((TokenNode) grammarObject).joinTokens());
			}
		}
		builder.append("\n]");
		return builder.toString();
	}
}
