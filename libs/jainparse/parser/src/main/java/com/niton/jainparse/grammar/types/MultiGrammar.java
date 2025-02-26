package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.SwitchNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.grammar.matchers.AnyOfMatcher;
import com.niton.jainparse.token.Tokenable;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Checks agains all given Grammars syncron and returns the first matching
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class MultiGrammar<T extends Enum<T> & Tokenable> extends WrapperGrammar<SwitchNode<T>,T> {
    private final AnyOfMatcher<T> matcher = new AnyOfMatcher<>(this);
    private Grammar<?,T>[] grammars;

    public MultiGrammar(Grammar<?,T>[] grammars) {
        this.grammars = grammars;
    }

    @Override
    protected Grammar<?,T> copy() {
        return new MultiGrammar<>(grammars);
    }

    /**
     * @see Grammar#createExecutor()
     */
    @Override
    public GrammarMatcher<SwitchNode<T>, T> createExecutor() {
        return matcher;
    }

    @Override
    public boolean isLeftRecursive(GrammarReference<T> ref) {
        return Arrays.stream(grammars).anyMatch(g -> g.isLeftRecursive(ref));
    }

    @Override
    public MultiGrammar<T> or(Grammar<?,T>... alternatives) {
        var combined = new Grammar<?,?>[alternatives.length + grammars.length];

        System.arraycopy(grammars, 0, combined, 0, grammars.length);
        System.arraycopy(alternatives, 0, combined, grammars.length, alternatives.length);
        return new MultiGrammar<T>((Grammar<?, T>[]) combined);
    }

    @Override
    protected Stream<Grammar<?,T>> getWrapped() {
        return Arrays.stream(grammars);
    }
}
