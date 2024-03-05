package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.grammar.matchers.NotMatcher;
import lombok.AllArgsConstructor;

import java.util.stream.Stream;

@AllArgsConstructor
public class NotGrammar extends WrapperGrammar<OptionalNode>  {
    private final Grammar<?> grammarNotToMatch;
    @Override
    protected Grammar<?> copy() {
        return new NotGrammar(grammarNotToMatch);
    }

    @Override
    protected GrammarMatcher<OptionalNode> createExecutor() {
        return new NotMatcher(grammarNotToMatch);
    }

    @Override
    public boolean isLeftRecursive(GrammarReference ref) {
        return grammarNotToMatch.isLeftRecursive(ref);
    }

    @Override
    protected Stream<Grammar<?>> getWrapped() {
        return Stream.of(grammarNotToMatch);
    }
}
