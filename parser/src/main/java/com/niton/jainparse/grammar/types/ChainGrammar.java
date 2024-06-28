package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.SequenceNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.WrapperGrammar;
import com.niton.jainparse.grammar.matchers.ChainMatcher;
import com.niton.jainparse.token.Tokenable;
import lombok.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Used to build a grammar<br>
 * Tests all Grammars in the chain after each other
 *
 * @author Nils
 * @version 2019-05-28
 */
@Getter
@NoArgsConstructor
public class ChainGrammar<T extends Enum<T> & Tokenable> extends WrapperGrammar<SequenceNode<T>,T> implements GrammarReference<T> {
    private final List<Grammar<?,T>> chain = new LinkedList<>();
    private final Map<Integer, String> naming = new HashMap<>();
    private boolean isLeftRecursive = false;

    public ChainGrammar(List<Grammar<?,T>> chain, Map<Integer, String> naming) {
        this.chain.addAll(chain);
        this.naming.putAll(naming);
    }

    public ChainGrammar<T> setLeftRecursive(boolean recursive) {
        isLeftRecursive = recursive;
        return this;
    }

    public void addGrammar(Grammar<?,T> grammar) {
        chain.add(grammar);
    }

    public void addGrammar(Grammar<?,T> grammar, String name) {
        chain.add(grammar);
        naming.put(chain.size() - 1, name);
    }

    @Override
    protected Grammar<?,T> copy() {
        return new ChainGrammar<>(chain, naming).setLeftRecursive(isLeftRecursive);
    }

    /**
     * @see Grammar#createExecutor()
     */
    @Override
    public ChainMatcher<T> createExecutor() {
        return new ChainMatcher(this);
    }

    @Override
    public boolean isLeftRecursive(GrammarReference ref) {
        return isLeftRecursive;
    }

    @Override
    protected Stream<Grammar<?>> getWrapped() {
        return chain.stream();
    }
    @Override
    public ChainGrammar then(Grammar<?> grammar) {
        if(getName() != null) return super.then(grammar);
        val newGram = new ChainGrammar(chain, naming);
        newGram.addGrammar(grammar);
        return newGram;
    }

    @Override
    public ChainGrammar then(String name, Grammar<?> grammar) {
        if(getName() != null) return super.then(grammar);
        val newGram = new ChainGrammar(chain, naming);
        newGram.addGrammar(grammar, name);

        return newGram;
    }

}
