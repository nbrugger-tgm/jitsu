package com.niton.parser.grammar.types;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.LocatableReducedNode;
import com.niton.parser.ast.ParsingResult;
import com.niton.parser.ast.TokenNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.api.WrapperGrammar;
import com.niton.parser.token.Location;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
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
                return grammar.parse(tokens, reference).map(r -> new TokenNode(r.join().collect(Collectors.toList()), tokens.currentLocation()));
            }
        };
    }

    @Override
    protected Stream<Grammar<?>> getWrapped() {
        return Stream.of(grammar);
    }
}
