package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
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
    }

    public boolean isPresent() {
        return value != null;
    }

    @Override
    public Stream<Tokenizer.AssignedToken> join() {
        return Optional.ofNullable(value).stream().flatMap(AstNode::join);
    }

    @Override
    public Optional<ReducedNode> reduce(@NonNull String name) {
        if(value != null)
            return value.reduce(name);
        else
            return Optional.empty();
    }
}

