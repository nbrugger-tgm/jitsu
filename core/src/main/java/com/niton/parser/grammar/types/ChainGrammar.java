package com.niton.parser.grammar.types;

import com.niton.parser.ast.SequenceNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.api.WrapperGrammar;
import com.niton.parser.grammar.matchers.ChainMatcher;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
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
public class ChainGrammar extends WrapperGrammar<SequenceNode>
        implements GrammarReference {
    private final List<Grammar<?>> chain = new LinkedList<>();
    private final Map<Integer, String> naming = new HashMap<>();

    public ChainGrammar(List<Grammar<?>> chain, Map<Integer, String> naming) {
        this.chain.addAll(chain);
        this.naming.putAll(naming);
    }

    public void addGrammar(Grammar<?> grammar) {
        chain.add(grammar);
    }

    public void addGrammar(Grammar<?> grammar, String name) {
        chain.add(grammar);
        naming.put(chain.size() - 1, name);
    }

    @Override
    protected Grammar<?> copy() {
        return new ChainGrammar(chain, naming);
    }

    /**
     * @see Grammar#createExecutor()
     */
    @Override
    public ChainMatcher createExecutor() {
        return new ChainMatcher(this);
    }

    @Override
    protected Stream<Grammar<?>> getWrapped() {
        return chain.stream();
    }
    @Override
    public Grammar<?> then(Grammar<?> grammar) {
        if(getName() != null) return super.then(grammar);
        val newGram = new ChainGrammar(chain, naming);
        newGram.addGrammar(grammar);
        return newGram;
    }

    @Override
    public Grammar<?> then(String name, Grammar<?> grammar) {
        if(getName() != null) return super.then(grammar);
        val newGram = new ChainGrammar(chain, naming);
        newGram.addGrammar(grammar, name);

        return newGram;
    }

}
