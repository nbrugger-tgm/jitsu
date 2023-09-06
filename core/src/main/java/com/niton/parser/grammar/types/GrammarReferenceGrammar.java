package com.niton.parser.grammar.types;

import com.niton.parser.ast.AstNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarName;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.matchers.ReferenceGrammarMatcher;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
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
public class GrammarReferenceGrammar extends Grammar<AstNode> {
    /**
     * -- GETTER --
     *
     * @return the grammar
     */
    @Getter
    private String grammar;
    private @Nullable String explicitName = null;
    public GrammarReferenceGrammar(String g) {
        this.grammar = g;
    }

    /**
     * @param grammar the grammar to set
     */
    public void setGrammar(String grammar) {
        this.grammar = grammar;
    }

    public Grammar<?> grammer(GrammarReference ref) {
        return ref.get(grammar);
    }

    @Override
    protected Grammar<?> copy() {
        return new GrammarReferenceGrammar(grammar);
    }

    @Override
    public ReferenceGrammarMatcher createExecutor() {
        if(grammar == null)
            throw new NullPointerException();
        return new ReferenceGrammarMatcher(grammar);
    }

    @Override
    public String getName() {
        return explicitName != null ? explicitName : getGrammar();
    }

	@Override
	public Grammar<AstNode> named(GrammarName name) {
		return setName(name.getName());
	}

    @Override
	public Grammar<AstNode> named(String name) {
		return setName(name);
	}

	@Override
    public Grammar<AstNode> setName(@Nullable String name) {
        this.explicitName = name;
        return this;
    }

    @Override
    public boolean isLeftRecursive(GrammarReference ref) {
        return Objects.requireNonNull(ref.get(grammar)).isLeftRecursive(ref);
    }
}
