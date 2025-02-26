package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenable;
import com.niton.jainparse.token.Tokenizer;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

public class KeywordMatcher<T extends Enum<T> & Tokenable> extends GrammarMatcher<TokenNode<T>,T> {
    private final String keyword;

    public KeywordMatcher(String keyword) {
        this.keyword = keyword;
    }

    @Override
    protected @NotNull ParsingResult<TokenNode<T>> process(@NotNull TokenStream<T> tokens, @NotNull GrammarReference<T> reference) {
        List<Tokenizer.AssignedToken<T>> collected = new LinkedList<>();
        StringBuilder collectedKeyword = new StringBuilder();
        var start = tokens.currentLocation();
        while (keyword.startsWith(collectedKeyword.toString())) {
            if (tokens.hasNext()) {
                var tkn = tokens.next();
                collected.add(tkn);
                collectedKeyword.append(tkn.getValue());
            } else {
                return ParsingResult.error(new ParsingException(
                        getIdentifier(),
                        "Expected keyword '"+keyword+"', got '"+exludeLinebreak(collectedKeyword)+"' and then EOF",
                        Location.range(start, tokens.currentLocation().minusChar(1))
                ));
            }
            if (collectedKeyword.toString().equals(keyword)) {
                return ParsingResult.ok(new TokenNode<>(collected, Location.range(start, tokens.currentLocation().minusChar(1))));
            }
        }
        return ParsingResult.error(new ParsingException(
                getIdentifier(),
                "Expected keyword '"+keyword+"', got '"+exludeLinebreak(collectedKeyword)+"'",
                Location.range(start, tokens.currentLocation().minusChar(1))
        ));
    }

    private String exludeLinebreak(StringBuilder collectedKeyword) {
        return collectedKeyword.toString().replace("\n", "\\n");
    }
}
