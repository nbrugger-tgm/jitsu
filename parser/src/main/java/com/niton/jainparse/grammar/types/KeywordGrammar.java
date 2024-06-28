package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.TokenNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.matchers.KeywordMatcher;
import com.niton.jainparse.token.Tokenable;


public class KeywordGrammar<T extends Enum<T> & Tokenable> extends Grammar<TokenNode<T>,T> {
    private final String keyword;

    public KeywordGrammar(String keyword) {
        this.keyword = keyword;
        setName(keyword);
    }

    @Override
    protected Grammar<?,T> copy() {
        return new KeywordGrammar<>(keyword);
    }

    @Override
    protected KeywordMatcher<T> createExecutor() {
        return new KeywordMatcher<>(keyword);
    }

    @Override
    public boolean isLeftRecursive(GrammarReference<T> ref) {
        return false;
    }
}
