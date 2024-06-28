package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.grammar.matchers.IgnoreMatcher;
import com.niton.jainparse.token.Tokenable;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Stream;

/**
 * This Grammar ignores the given grammar
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class IgnoreGrammar<T extends Enum<T> & Tokenable> extends WrapperGrammar<OptionalNode<T>, T> {
    private final Grammar<?,T> grammar;
    private final IgnoreMatcher matcher;

    public IgnoreGrammar(Grammar<?,T> grammar) {
        this.grammar = grammar;
        this.matcher = new IgnoreMatcher(grammar);
    }


    @Override
    protected Stream<Grammar<?,T>> getWrapped() {
        return Stream.of(grammar);
    }

    @Override
    protected Grammar<?,T> copy() {
        return new IgnoreGrammar<>(grammar);
    }

    /**
     * @see Grammar#createExecutor()
     */
    @Override
    public IgnoreMatcher<T> createExecutor() {
        return matcher;
    }

    @Override
    public boolean isLeftRecursive(GrammarReference ref) {
        return grammar.isLeftRecursive(ref);
    }

}
