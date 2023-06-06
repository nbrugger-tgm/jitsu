package com.niton.parser.ast;

import lombok.NonNull;

import java.util.Optional;

public class IgnoredNode extends OptionalNode{
    @Override
    public Optional<LocatableReducedNode> reduce(@NonNull String name) {
        return Optional.empty();
    }
}
