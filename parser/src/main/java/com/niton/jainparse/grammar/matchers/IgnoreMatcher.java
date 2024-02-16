package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.ast.IgnoredNode;
import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.token.TokenStream;
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
public class IgnoreMatcher extends GrammarMatcher<OptionalNode> {
    private Grammar<?> grammar;

    public IgnoreMatcher(Grammar<?> name2) {
        this.grammar = name2;
    }


    /**
     * @param tokens
     * @param ref
     * @see GrammarMatcher#process(TokenStream, GrammarReference)
     */
    @Override
    public @NotNull ParsingResult<OptionalNode> process(@NotNull TokenStream tokens, @NotNull GrammarReference ref) {
        OptionalNode thisRes = grammar.parse(tokens, ref).map(
                (sub) -> new IgnoredNode(tokens.currentLocation(), sub.getParsingException())
        ).orElse(
                err -> new IgnoredNode(tokens.currentLocation(), new ParsingException(getIdentifier(), "Nothing to ignore", err))
        );
        return ParsingResult.ok(thisRes);
    }
}
