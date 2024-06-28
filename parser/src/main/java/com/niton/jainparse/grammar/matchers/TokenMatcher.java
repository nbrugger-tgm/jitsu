package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.types.TokenGrammar;
import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenable;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Simply matches a token
 *
 * @author Nils
 * @version 2019-05-28
 */
public class TokenMatcher<T extends Enum<T> & Tokenable> extends GrammarMatcher<TokenNode<T>,T> {
    private final TokenGrammar<?> grammar;

    public TokenMatcher(TokenGrammar<T> grammar) {
        this.grammar = grammar;
    }

    @Override
    public @NotNull ParsingResult<TokenNode<T>> process(@NotNull TokenStream<T> tokens, @NotNull GrammarReference<T> ref) {
        var start = tokens.currentLocation();
        if (!tokens.hasNext()) {
            return ParsingResult.error(new ParsingException(
                    getIdentifier(),
                    String.format(
                            "Expected \"%s\" but found nothing (whole file parsed already)",
                            grammar.getTokenName()
                    ),
                    start
            ));
        }
        AssignedToken<T> token = tokens.next();
        var tokenRange = Location.range(start, Location.of(
                start.getFromLine(),
                start.getFromColumn(),
                tokens.currentLocation().getToLine(),
                tokens.currentLocation().getToColumn() - 1
        ));
        if (token.getType().equals(grammar.getTokenName())) {
            return ParsingResult.ok(new TokenNode<>(List.of(token), tokenRange));
        }
        return ParsingResult.error(new ParsingException(getIdentifier(), String.format(
                "expected \"%s\" but got \"%s\"",
                grammar.getTokenName(),
                token.getType()
        ), tokenRange));
    }

    /**
     * @return the tokenName
     */
    public String getGrammar() {
        return grammar.getTokenName();
    }
}
