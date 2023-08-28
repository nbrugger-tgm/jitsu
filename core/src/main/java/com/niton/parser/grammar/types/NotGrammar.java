package com.niton.parser.grammar.types;

import com.niton.parser.ast.OptionalNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.WrapperGrammar;
import com.niton.parser.grammar.matchers.NotMatcher;
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
    protected Stream<Grammar<?>> getWrapped() {
        return Stream.of(grammarNotToMatch);
    }
}
