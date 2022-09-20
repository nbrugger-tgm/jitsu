package com.niton.parser.grammar.types;

import com.niton.parser.ast.IgnoredNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.matchers.NotMatcher;

public class NoMatchGrammer extends Grammar<NotMatcher, IgnoredNode> {
    private final Grammar<?, ?> notToMatch;

    public NoMatchGrammer(Grammar<?, ?> notToMatch) {
        this.notToMatch = notToMatch;
    }

    @Override
    protected NotMatcher createExecutor() {
        return new NotMatcher(this.notToMatch);
    }
}
