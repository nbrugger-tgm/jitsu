package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.ParsingResult;
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
	protected @NotNull ParsingResult<AstNode> process(@NotNull TokenStream tokens, @NotNull GrammarReference ref) {
		Grammar<?> g = ref.get(grammar);
		if (g == null) {
			return ParsingResult.error(new ParsingException(getIdentifier(), format(
					"Unknown reference! The Grammar \"%s\" was not found in reference",
					grammar
			),tokens.currentLocation()));
		}
		var res = g.parse(tokens, ref);
		if(res.wasParsed()){
			return ParsingResult.ok(res.unwrap());
		} else {
			return ParsingResult.error(res.exception());
		}
	}
}
