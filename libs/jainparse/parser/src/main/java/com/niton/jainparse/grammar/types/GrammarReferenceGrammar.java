package com.niton.jainparse.grammar.types;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarName;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.matchers.ReferenceGrammarMatcher;
import com.niton.jainparse.token.Tokenable;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This is the GrammarMatchGrammar Class
 *
 * @author Nils Brugger
 * @version 2019-06-05
 */
@Getter
@Setter
public class GrammarReferenceGrammar<T extends Enum<T> & Tokenable> extends Grammar<AstNode<T>,T> {
    @Setter
    @Getter
    private String grammar;
    private @Nullable String explicitName = null;
    public GrammarReferenceGrammar(String g) {
        this.grammar = g;
    }

    @Override
    protected Grammar<AstNode<T>,T> copy() {
        return new GrammarReferenceGrammar<>(grammar);
    }

    @Override
    public GrammarMatcher<AstNode<T>,T> createExecutor() {
        if(grammar == null)
            throw new NullPointerException();
        return new ReferenceGrammarMatcher<>(grammar);
    }

    @Override
    public String getName() {
        return explicitName != null ? explicitName : getGrammar();
    }

	@Override
	public Grammar<AstNode<T>,T> named(GrammarName name) {
		return setName(name.getName());
	}

    @Override
	public Grammar<AstNode<T>,T> named(String name) {
		return setName(name);
	}

	@Override
    public Grammar<AstNode<T>,T> setName(@Nullable String name) {
        this.explicitName = name;
        return this;
    }

    @Override
    public boolean isLeftRecursive(GrammarReference<T> ref) {
        return Objects.requireNonNull(ref.get(grammar)).isLeftRecursive(ref);
    }
}
