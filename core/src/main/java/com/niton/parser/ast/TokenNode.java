package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer.AssignedToken;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A node that represents a sequence of tokens that were matched
 */
@Data
@EqualsAndHashCode(callSuper = true)
@RequiredArgsConstructor
public class TokenNode extends AstNode {
	private final List<AssignedToken> tokens;
	private final Location loc;

	@Override
	public Location getLocation() {
		return loc;
	}

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
	public Stream<AssignedToken> join() {
		return tokens.stream();
	}

	@Override
	public Optional<LocatableReducedNode> reduce(@NotNull String name) {
		return Optional.of(LocatableReducedNode.leaf(name, joinTokens(),getLocation()));
	}
}
