package com.niton.jainparse.grammar.api;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.token.Tokenable;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class WrapperGrammar<T extends AstNode<TKN>, TKN extends Enum<TKN> & Tokenable> extends Grammar<T, TKN> {

    protected abstract Stream<Grammar<?, TKN>> getWrapped();

    private Stream<Grammar<?, TKN>> getAllWrapped() {
        return Stream.concat(getWrapped(), getWrapped().flatMap(g -> g instanceof WrapperGrammar ? ((WrapperGrammar<?, TKN>) g).getAllWrapped() : Stream.empty()));
    }

//    @Override
//    public @Nullable Grammar<?, TKN> get(String key) {
//        if (getName().equals(key))
//            return this;
//        return getAllWrapped().filter(g -> g.getName() != null && g.getName().equals(key)).findFirst().orElse(null);
//    }
//
//    @Override
//    public Set<String> grammarNames() {
//        return Stream.concat(getAllWrapped(), Stream.of(this))
//                .map(Grammar::getName)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//    }
}
