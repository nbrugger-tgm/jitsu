package com.niton.jainparse.ast;

import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.api.Location;
import lombok.NonNull;

import java.util.Optional;

public class IgnoredNode extends OptionalNode{
    public IgnoredNode(Location location, ParsingException ex) {
        super(location, ex);
    }

    @Override
    public Optional<LocatableReducedNode> reduce(@NonNull String name) {
        return Optional.empty();
    }
}
