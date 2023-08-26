package com.niton.parser.grammar.types;

import com.niton.parser.ast.OptionalNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.WrapperGrammar;
import com.niton.parser.grammar.matchers.IgnoreMatcher;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

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
    private Grammar<?> grammar;

    public IgnoreGrammar(Grammar<?> grammar) {
        this.grammar = grammar;
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
        return new IgnoreMatcher(grammar);
    }

}
