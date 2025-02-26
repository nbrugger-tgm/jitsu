package com.niton.jainparse.grammar.types;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenable;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MergedGrammar<T extends Enum<T> & Tokenable> extends WrapperGrammar<TokenNode<T>,T> {
    private final Grammar<?,T> grammar;

    public MergedGrammar(Grammar<?,T> rGrammar) {
        this.grammar = rGrammar;
    }

    @Override
    protected MergedGrammar<T> copy() {
        return new MergedGrammar<>(grammar);
    }

    @Override
    protected GrammarMatcher<TokenNode<T>,T> createExecutor() {
        return new GrammarMatcher<>() {
            @Override
            protected @NotNull ParsingResult<TokenNode<T>> process(@NotNull TokenStream<T> tokens, @NotNull GrammarReference<T> reference) {
                return grammar.parse(tokens, reference).map(r -> new TokenNode<>(r.join().collect(Collectors.toList()), r.getLocation()));
            }
        };
    }

    @Override
    public boolean isLeftRecursive(GrammarReference<T> ref) {
        return grammar.isLeftRecursive(ref);
    }

    @Override
    protected Stream<Grammar<?,T>> getWrapped() {
        return Stream.of(grammar);
    }
}
