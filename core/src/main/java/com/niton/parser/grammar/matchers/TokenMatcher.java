package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.ParsingResult;
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
    public @NotNull ParsingResult<TokenNode> process(@NotNull TokenStream tokens, @NotNull GrammarReference ref) {
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
        AssignedToken token = tokens.next();
        var tokenRange = Location.range(start, Location.of(
                start.getFromLine(),
                start.getFromColumn(),
                tokens.currentLocation().getToLine(),
                tokens.currentLocation().getToColumn() - 1
        ));
        if (token.getName().equals(grammar.getTokenName())) {
            return ParsingResult.ok(new TokenNode(List.of(token), tokenRange));
        }
        return ParsingResult.error(new ParsingException(getIdentifier(), String.format(
                "expected \"%s\" but got \"%s\"",
                grammar.getTokenName(),
                token.getName()
        ), tokenRange));
    }

    /**
     * @return the tokenName
     */
    public String getGrammar() {
        return grammar.getTokenName();
    }

}
