package com.niton.parser.ast;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Tokenizer;
import lombok.Getter;
import lombok.NonNull;
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
	private ParsingException parsingException;

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

	/**
	 * Reduces the node to the bare minimum. Usefully for interpreting the AST.
	 * it gets rid of unnecessary layers, filters not needed nodes and condenses
	 * AST nodes into strings when needed.
	 * <p>
	 * Example:
	 * <pre>
	 *    grammar : repeat(optional(grammar(token(LETTER)));
	 *    ast node: repeat_node([OptionalNode(GrammarReferenceNode(TokenNode("someLetters")))])
	 *    reduced : repeat_node(["someLetters"])
	 * </pre>
	 * so reducing removes all intermediate steps to get to the value since {@code OptionalNode},
	 * {@code GrammarReferenceNode} and {@code TokenNode} are not important for getting values out
	 * of the AST.
	 * </p>
	 * <p>
	 * <b>Contract:</b></br>
	 * The exact implementation is of course dependent on each node since "what you need for
	 * interpreting the AST" is not a 100% clear definition.
	 * <ul>
	 *  <li>Named content is never discarded</li>
	 *  <li>When nothing is relevant null is returned</li>
	 *  <li>You can proxy/forward other `reduce()` results</li>
	 * </ul>
	 * </p>
	 *
	 * @param name the name the returned node is given. This is important since retrieving nodes
	 *             from the tree is done by their name and its alway the parent node who defines
	 *             the name of a child. The name can be a regular identifier or a numeric value (as
	 *             string)
	 *
	 * @return a reduced form of this node or {@code null} when the node has no content worth interpreting
	 */
	public abstract ReducedNode reduce(@NonNull String name);
}

