package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.SequenceNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.grammar.matchers.RepeatMatcher;
import com.niton.jainparse.token.Tokenable;
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
public class RepeatGrammar<T extends Enum<T> & Tokenable> extends WrapperGrammar<SequenceNode<T>, T> {
    private Grammar<?,T> check;
    private final int minimum;
    public RepeatGrammar(Grammar<?,T> gramarReference, int minimum) {
        this.check = gramarReference;
        this.minimum = minimum;
    }

    @Override
    protected Stream<Grammar<?,T>> getWrapped() {
        return Stream.of(check);
    }

    @Override
    protected Grammar<?,T> copy() {
        return new RepeatGrammar<T>(check, minimum);
    }

    /**
     * @see Grammar#createExecutor()
     */
    @Override
    public RepeatMatcher<T> createExecutor() {
        return new RepeatMatcher<>(check, minimum);
    }

    @Override
    public boolean isLeftRecursive(GrammarReference<T> ref) {
        return check.isLeftRecursive(ref);
    }
}
