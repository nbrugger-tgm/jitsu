package com.niton.parser.ast;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Location;
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
