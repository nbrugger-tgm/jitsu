package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.SequenceNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.grammar.matchers.RepeatMatcher;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Stream;

/**
 * Checks the grammar as often as is occurs
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class RepeatGrammar extends WrapperGrammar<SequenceNode> {
    private Grammar<?> check;
    private final int minimum;
    public RepeatGrammar(Grammar<?> gramarReference, int minimum) {
        this.check = gramarReference;
        this.minimum = minimum;
    }

    @Override
    protected Stream<Grammar<?>> getWrapped() {
        return Stream.of(check);
    }

    @Override
    protected Grammar<?> copy() {
        return new RepeatGrammar(check, minimum);
    }

    /**
     * @see Grammar#createExecutor()
     */
    @Override
    public RepeatMatcher createExecutor() {
        return new RepeatMatcher(check, minimum);
    }

    @Override
    public boolean isLeftRecursive(GrammarReference ref) {
        return check.isLeftRecursive(ref);
    }
}
