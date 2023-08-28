package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.OptionalNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.Location;
import com.niton.parser.token.TokenStream;
import lombok.AllArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class NotMatcher extends GrammarMatcher<OptionalNode> {
    private final Grammar<?> grammarNotToMatch;

    @NotNull
    @Override
    protected OptionalNode process(@NotNull TokenStream tokens, @NotNull GrammarReference reference) throws ParsingException {
        tokens.elevate();
        var start = tokens.currentLocation();
        AstNode result;
        try {
            result = grammarNotToMatch.parse(tokens, reference);
        } catch (ParsingException e) {
            tokens.commit();
            var node = new OptionalNode();
            node.setParsingException(new ParsingException(getIdentifier(), "Successfully  mis-matched " + grammarNotToMatch.getIdentifier(), e));
            return node;
        }
        var exception = result.getParsingException();
        tokens.rollback();
        if (exception != null)
            throw new ParsingException(getIdentifier(), "Expected not to match " + grammarNotToMatch.getIdentifier(), exception);
        else throw new ParsingException(
                getIdentifier(),
                "Expected not to match " + grammarNotToMatch.getIdentifier(),
                Location.range(start, tokens.currentLocation())
        );
    }
}
