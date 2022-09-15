package com.niton.parser.grammar.types;

import com.niton.parser.ast.TokenNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.matchers.KeywordMatcher;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KeywordGrammar extends Grammar<KeywordMatcher, TokenNode> {
    private final String keyword;
    @Override
    protected KeywordMatcher createExecutor() {
        return new KeywordMatcher(keyword);
    }
}
