package com.niton.jainparse.ast;

import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.Tokenable;
import com.niton.jainparse.token.Tokenizer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A node that contains the result of a grammar that can optionally match or not.
 * If it matches, it contains the result of the match.
 */
public class OptionalNode<T extends Enum<T> & Tokenable> extends AstNode<T> {
    @Nullable
	private final AstNode<T> value;

    private final Location location;

    public OptionalNode(AstNode<T> valueNode) {
        value = valueNode;
        location = valueNode.getLocation();
        setParsingException(valueNode.getParsingException());
    }

    public OptionalNode(Location location, ParsingException ex) {
        this.value = null;
        this.location = location;
        setParsingException(ex);
    }

	@Override
    public String toString() {
        return "[optional]";
    }

    public Optional<AstNode<T>> getValue() {
        return Optional.ofNullable(value);
    }

    public boolean isPresent() {
        return value != null;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public Stream<Tokenizer.AssignedToken<T>> join() {
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

