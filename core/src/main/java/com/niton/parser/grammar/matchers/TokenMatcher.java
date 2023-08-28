package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.TokenGrammar;
import com.niton.parser.token.Location;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer.AssignedToken;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Simply matches a token
 *
 * @author Nils
 * @version 2019-05-28
 */
public class TokenMatcher extends GrammarMatcher<TokenNode> {
    private final TokenGrammar grammar;

    public TokenMatcher(TokenGrammar grammar) {
        this.grammar = grammar;
    }

    @Override
    public @NotNull TokenNode process(@NotNull TokenStream tokens, @NotNull GrammarReference ref)
            throws ParsingException {
        var start = tokens.currentLocation();
        if (!tokens.hasNext()) {
            throw new ParsingException(
                    getIdentifier(),
                    String.format(
                            "Expected \"%s\" but found nothing (whole file parsed already)",
                            grammar.getTokenName()
                    ),
                    start
            );
        }
        AssignedToken token = tokens.next();
        var tokenRange = Location.range(start, tokens.currentLocation());
        if (token.getName().equals(grammar.getTokenName())) {
            return new TokenNode(List.of(token), tokenRange);
        }
        throw new ParsingException(getIdentifier(), String.format(
                "expected \"%s\" but got \"%s\"",
                grammar.getTokenName(),
                token.getName()
        ), tokenRange);
    }

    /**
     * @return the tokenName
     */
    public String getGrammar() {
        return grammar.getTokenName();
    }

}
