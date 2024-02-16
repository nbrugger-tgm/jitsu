package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.grammar.matchers.IgnoreMatcher;
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
public class IgnoreGrammar extends WrapperGrammar<OptionalNode> {
    private final Grammar<?> grammar;
    private final IgnoreMatcher matcher;

    public IgnoreGrammar(Grammar<?> grammar) {
        this.grammar = grammar;
        this.matcher = new IgnoreMatcher(grammar);
    }


    @Override
    protected Stream<Grammar<?>> getWrapped() {
        return Stream.of(grammar);
    }

    @Override
    protected Grammar<?> copy() {
        return new IgnoreGrammar(grammar);
    }

    /**
     * @see Grammar#createExecutor()
     */
    @Override
    public IgnoreMatcher createExecutor() {
        return matcher;
    }

    @Override
    public boolean isLeftRecursive(GrammarReference ref) {
        return grammar.isLeftRecursive(ref);
    }

}
