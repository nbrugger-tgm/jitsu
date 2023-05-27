package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.IgnoredNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.TokenStream;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class NotMatcher extends GrammarMatcher<IgnoredNode> {
    private final Grammar<?, ?> grammar;

    @Override
    protected @NotNull IgnoredNode process(@NotNull TokenStream tokens, @NotNull GrammarReference reference) throws ParsingException {
        try {
            var result = grammar.parse(tokens, reference);
            throw new ParsingException(getIdentifier(), String.format("Expected not to match '%s'", result.joinTokens()), tokens);
        } catch (ParsingException e) {
            return new IgnoredNode();
        }
    }
}
