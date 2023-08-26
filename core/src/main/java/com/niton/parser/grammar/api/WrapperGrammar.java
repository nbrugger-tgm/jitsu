package com.niton.parser.grammar.api;

import com.niton.parser.ast.AstNode;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class WrapperGrammar<T extends AstNode> extends Grammar<T> implements GrammarReference {

    protected abstract Stream<Grammar<?>> getWrapped();

    private Stream<Grammar<?>> getAllWrapped() {
        return Stream.concat(getWrapped(), getWrapped().flatMap(g -> g instanceof WrapperGrammar ? ((WrapperGrammar<?>) g).getAllWrapped() : Stream.empty()));
    }

    @Override
    public @Nullable Grammar<?> get(String key) {
        if (getName().equals(key))
            return this;
        return getAllWrapped().filter(g -> g.getName() != null && g.getName().equals(key)).findFirst().orElse(null);
    }

    @Override
    public Set<String> grammarNames() {
        return Stream.concat(getAllWrapped(), Stream.of(this))
                .map(Grammar::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
