package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

public class KeywordMatcher extends GrammarMatcher<TokenNode> {
    private final String keyword;

    public KeywordMatcher(String keyword) {
        this.keyword = keyword;
    }

    @Override
    protected @NotNull TokenNode process(@NotNull TokenStream tokens, @NotNull GrammarReference reference) throws ParsingException {
        List<Tokenizer.AssignedToken> collected = new LinkedList<>();
        StringBuilder collectedKeyword = new StringBuilder();
        int startLine = tokens.getLine();
        int startColumn = tokens.getColumn();
        while (keyword.startsWith(collectedKeyword.toString())) {
            if (tokens.hasNext()) {
                var tkn = tokens.next();
                collected.add(tkn);
                collectedKeyword.append(tkn.getValue());
            } else {
                throw new ParsingException(getIdentifier(), format("Expected keyword '%s', got EOF", keyword), tokens);
            }
            if (collectedKeyword.toString().equals(keyword)) {
                AstNode.Location location = AstNode.Location.of(startLine, startColumn, tokens.getLine(), tokens.getColumn());
                return new TokenNode(collected, location);
            }
        }
        throw new ParsingException(getIdentifier(), format("Expected keyword '%s', got '%s'", keyword, collectedKeyword), tokens);
    }
}
