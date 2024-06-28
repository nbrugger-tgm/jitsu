package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.grammar.matchers.NotMatcher;
import com.niton.jainparse.token.Tokenable;
import lombok.AllArgsConstructor;

import java.util.stream.Stream;

@AllArgsConstructor
public class NotGrammar<T extends Enum<T> & Tokenable> extends WrapperGrammar<OptionalNode<T>,T>  {
    private final Grammar<?,T> grammarNotToMatch;
    @Override
    protected Grammar<?,T> copy() {
        return new NotGrammar<>(grammarNotToMatch);
    }

    @Override
    protected GrammarMatcher<OptionalNode<T>,T> createExecutor() {
        return new NotMatcher<>(grammarNotToMatch);
    }

    @Override
    public boolean isLeftRecursive(GrammarReference<T> ref) {
        return grammarNotToMatch.isLeftRecursive(ref);
    }

    @Override
    protected Stream<Grammar<?,T>> getWrapped() {
        return Stream.of(grammarNotToMatch);
    }
}
