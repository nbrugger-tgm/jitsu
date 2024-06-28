package com.niton.jainparse.ast;

import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.Tokenable;
import lombok.NonNull;

import java.util.Optional;

public class IgnoredNode<T extends Enum<T> & Tokenable> extends OptionalNode<T>{
    public IgnoredNode(Location location, ParsingException ex) {
        super(location, ex);
    }

    @Override
    public Optional<LocatableReducedNode> reduce(@NonNull String name) {
        return Optional.empty();
    }
}
