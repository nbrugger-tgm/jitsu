package com.niton.parser.grammar.types;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.matchers.KeywordMatcher;
import org.jetbrains.annotations.NotNull;


public class KeywordGrammar extends Grammar<TokenNode> {
    private final String keyword;

    public KeywordGrammar(String keyword) {
        this.keyword = keyword;
        setName(keyword);
    }

    @Override
    protected Grammar<?> copy() {
        return new KeywordGrammar(keyword);
    }

    @Override
    protected KeywordMatcher createExecutor() {
        return new KeywordMatcher(keyword);
    }
}
