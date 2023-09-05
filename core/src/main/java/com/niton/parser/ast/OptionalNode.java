package com.niton.parser.ast;

import com.niton.parser.token.Location;
import com.niton.parser.token.Tokenizer;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A node that contains the result of a grammar that can optionally match or not.
 * If it matches, it contains the result of the match.
 */
@NoArgsConstructor
public class OptionalNode extends AstNode {
    @Nullable
	private AstNode value;

    public OptionalNode(@Nullable AstNode valueNode) {
        value = valueNode;
    }

	@Override
    public String toString() {
        return "[optional]";
    }

    public Optional<AstNode> getValue() {
        return Optional.ofNullable(value);
    }

    public void setValue(@Nullable AstNode value) {
        this.value = value;
        this.setParsingException(value == null ? null : value.getParsingException());
    }

    public boolean isPresent() {
        return value != null;
    }

    @Override
    public Location getLocation() {
        return new Location() {
            @Override
            public int getFromLine() {
                return value == null ? 1 : value.getLocation().getFromLine();
            }

            @Override
            public int getFromColumn() {
                return value == null ? 1 : value.getLocation().getFromColumn();
            }

            @Override
            public int getToLine() {
                return value == null ? 1 : value.getLocation().getToLine();
            }

            @Override
            public int getToColumn() {
                return value == null ? 1 : value.getLocation().getToColumn();
            }
        };
    }

    @Override
    public Stream<Tokenizer.AssignedToken> join() {
        return Optional.ofNullable(value).stream().flatMap(AstNode::join);
    }

    @Override
    public Optional<LocatableReducedNode> reduce(@NonNull String name) {
        if(value != null)
            return value.reduce(name);
        else
            return Optional.empty();
    }
}

