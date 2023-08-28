package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.TokenStream;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;

/**
 * This is the GrammarMatchGrammar Class
 *
 * @author Nils Brugger
 * @version 2019-06-05
 */
public class ReferenceGrammarMatcher extends GrammarMatcher<AstNode> {

	private String grammar;

	public ReferenceGrammarMatcher(String grammar) {
		super();
		this.grammar = grammar;
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

	@Override
	protected @NotNull AstNode process(@NotNull TokenStream tokens, @NotNull GrammarReference ref)
			throws ParsingException {
		if (ref.get(grammar) == null) {
			throw new ParsingException(getIdentifier(), format(
					"Unknown reference! The Grammar \"%s\" was not found in reference",
					grammar
			),tokens.currentLocation());
		}
		Grammar<?> g = ref.get(grammar);
		return g.parse(tokens, ref);
	}
}
