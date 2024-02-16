package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.matchers.KeywordMatcher;


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

    @Override
    public boolean isLeftRecursive(GrammarReference ref) {
        return false;
    }
}
