package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.ParsingResult;
import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.Location;
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
    protected @NotNull ParsingResult<TokenNode> process(@NotNull TokenStream tokens, @NotNull GrammarReference reference) {
        List<Tokenizer.AssignedToken> collected = new LinkedList<>();
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
                        Location.range(start, tokens.currentLocation())
                ));
            }
            if (collectedKeyword.toString().equals(keyword)) {
                return ParsingResult.ok(new TokenNode(collected, Location.range(start, tokens.currentLocation())));
            }
        }
        return ParsingResult.error(new ParsingException(
                getIdentifier(),
                "Expected keyword '"+keyword+"', got '"+exludeLinebreak(collectedKeyword)+"'",
                Location.range(start, tokens.currentLocation())
        ));
    }

    private String exludeLinebreak(StringBuilder collectedKeyword) {
        return collectedKeyword.toString().replace("\n", "\\n");
    }
}
