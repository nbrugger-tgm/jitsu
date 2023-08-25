package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.TokenGrammar;
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
    private TokenGrammar tokenName;

    /**
     * Creates an Instance of TokenGrammar.java
     *
     * @param tokenName
     * @author Nils
     * @version 2019-05-28
     */
    public TokenMatcher(TokenGrammar tokenName) {
        this.tokenName = tokenName;
    }

    @Override
    public @NotNull TokenNode process(@NotNull TokenStream tokens, @NotNull GrammarReference ref)
            throws ParsingException {
        if (!tokens.hasNext()) {
            throw new ParsingException(
                    getIdentifier(),
                    String.format(
                            "Expected \"%s\" but found nothing (whole file parsed already)",
                            tokenName.getTokenName()
                    ),
                    tokens
            );
        }
        int startLine = tokens.getLine();
        int startColumn = tokens.getColumn();
        int startIndex = tokens.index();
        AssignedToken token = tokens.next();
        if (token.getName().equals(tokenName.getTokenName())) {
            AstNode.Location location = AstNode.Location.of(startLine, startColumn, tokens.getLine(), tokens.getColumn());
            return new TokenNode(List.of(token), location);
        }
        throw new ParsingException(getIdentifier(), String.format(
                "expected \"%s\" but got \"%s\"",
                tokenName.getTokenName(),
                token.getName()
        ), startLine, startColumn, startIndex);
    }

    /**
     * @return the tokenName
     */
    public String getTokenName() {
        return tokenName.getTokenName();
    }

    /**
     * @param tokenName the tokenName to set
     */
    public void setTokenName(TokenGrammar tokenName) {
        this.tokenName = tokenName;
    }
}
