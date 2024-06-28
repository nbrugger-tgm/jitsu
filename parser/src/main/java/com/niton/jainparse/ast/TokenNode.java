package com.niton.jainparse.ast;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.Tokenable;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
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
public class TokenNode<T extends Enum<T> & Tokenable> extends AstNode<T> {
	private final List<AssignedToken<T>> tokens;
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
		for (AssignedToken<T> grammarObject : tokens) {
			builder.append(grammarObject.getValue());
		}
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for (AssignedToken<T> t : tokens) {
			builder.append("\n\t");
			builder.append(t.toString());
		}
		builder.append("\n]");
		return builder.toString();
	}

	@Override
	public Stream<AssignedToken<T>> join() {
		return tokens.stream();
	}

	@Override
	public Optional<LocatableReducedNode> reduce(@NotNull String name) {
		return Optional.of(LocatableReducedNode.leaf(name, joinTokens(),getLocation()));
	}
}
