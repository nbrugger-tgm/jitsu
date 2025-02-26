package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenable;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * This Grammar ignores the given grammar
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class IgnoreMatcher<T extends Enum<T> & Tokenable> extends GrammarMatcher<OptionalNode<T>,T> {
    private Grammar<?,T> grammar;

    public IgnoreMatcher(Grammar<?,T> name2) {
        this.grammar = name2;
    }


    /**
     * @param tokens
     * @param ref
     * @see GrammarMatcher#process(TokenStream, GrammarReference)
     */
    @Override
    public @NotNull ParsingResult<OptionalNode<T>> process(@NotNull TokenStream<T> tokens, @NotNull GrammarReference<T> ref) {
        OptionalNode<T> thisRes = grammar.parse(tokens, ref).map(
                (sub) -> new OptionalNode<T>(tokens.currentLocation(), sub.getParsingException())
        ).orElse(
                err -> new OptionalNode<>(tokens.currentLocation(), new ParsingException(getIdentifier(), "Nothing to ignore", err))
        );
        return ParsingResult.ok(thisRes);
    }
}
