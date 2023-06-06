package com.niton.parser.grammar.types;

import com.niton.parser.ast.AstNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarName;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.matchers.ReferenceGrammarMatcher;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/**
 * This is the GrammarMatchGrammar Class
 *
 * @author Nils Brugger
 * @version 2019-06-05
 */
@Getter
@Setter
public class GrammarReferenceGrammar extends Grammar<AstNode> {
    private String grammar;

    public GrammarReferenceGrammar(String g) {
        this.grammar = g;
    }

    /**
     * @return the grammar
     */
    public String getGrammar() {
        return grammar;
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

    /**
     * @return
     * @see Grammar#createExecutor()
     */
    @Override
    public ReferenceGrammarMatcher createExecutor() {
        return new ReferenceGrammarMatcher(grammar);
    }

    @Override
    public String getName() {
        return getGrammar();
    }

	@Override
	public Grammar<AstNode> named(GrammarName name) {
		return setName(null);
	}

	@Override
	public Grammar<AstNode> named(String name) {
		return setName(null);
	}

	@Override
    public Grammar<AstNode> setName(@Nullable String name) {
        throw new UnsupportedOperationException("Naming a GrammarReferenceGrammar is not useful. Instead name the referenced grammar!");
    }
}
