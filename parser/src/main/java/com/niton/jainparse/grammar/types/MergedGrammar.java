package com.niton.jainparse.grammar.types;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.token.TokenStream;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MergedGrammar extends WrapperGrammar<TokenNode> {
    private final Grammar<?> grammar;

    public MergedGrammar(Grammar<?> rGrammar) {
        this.grammar = rGrammar;
    }

    @Override
    protected MergedGrammar copy() {
        return new MergedGrammar(grammar);
    }

    @Override
    protected GrammarMatcher<TokenNode> createExecutor() {
        return new GrammarMatcher<>() {
            @Override
            protected @NotNull ParsingResult<TokenNode> process(@NotNull TokenStream tokens, @NotNull GrammarReference reference) {
                return grammar.parse(tokens, reference).map(r -> new TokenNode(r.join().collect(Collectors.toList()), r.getLocation()));
            }
        };
    }

    @Override
    public boolean isLeftRecursive(GrammarReference ref) {
        return grammar.isLeftRecursive(ref);
    }

    @Override
    protected Stream<Grammar<?>> getWrapped() {
        return Stream.of(grammar);
    }
}
