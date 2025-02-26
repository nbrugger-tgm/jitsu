package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenable;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Cheks if the grammar is right if yes it adds the element to the output if not
 * it is ignored
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class OptionalMatcher<T extends Enum<T> & Tokenable> extends GrammarMatcher<OptionalNode<T>,T> {

    private Grammar<?,T> check;

    public OptionalMatcher(Grammar<?,T> value) {
        this.check = value;
    }


    /**
     * @param tokens
     * @param ref
     * @see GrammarMatcher#process(TokenStream, GrammarReference)
     */
    @Override
    public @NotNull ParsingResult<OptionalNode<T>> process(@NotNull TokenStream<T> tokens, @NotNull GrammarReference<T> ref) {
        return ParsingResult.ok(
                check.parse(tokens, ref).map(
                        OptionalNode::new
                ).orElse((ex) -> new OptionalNode<>(tokens.currentLocation(), ex))
        );
    }

}
