package com.niton.parser.grammar.types;

import com.niton.parser.ast.SwitchNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.api.WrapperGrammar;
import com.niton.parser.grammar.matchers.AnyOfMatcher;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Checks agains all given Grammars syncron and returns the first matching
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class MultiGrammar extends WrapperGrammar<SwitchNode> {
    private Grammar<?>[] grammars;

    public MultiGrammar(Grammar<?>[] grammars) {
        this.grammars = grammars;
    }

    @Override
    protected Grammar<?> copy() {
        return new MultiGrammar(grammars);
    }

    /**
     * @see Grammar#createExecutor()
     */
    @Override
    public AnyOfMatcher createExecutor() {
        return new AnyOfMatcher(this);
    }

    @Override
    public MultiGrammar or(Grammar<?>... alternatives) {
        var combined = new Grammar<?>[alternatives.length + grammars.length];

        System.arraycopy(grammars, 0, combined, 0, grammars.length);
        System.arraycopy(alternatives, 0, combined, grammars.length, alternatives.length);
        var cloned = new MultiGrammar(combined);
        return cloned;
    }


    @Override
    protected Stream<Grammar<?>> getWrapped() {
        return Arrays.stream(grammars);
    }
}
