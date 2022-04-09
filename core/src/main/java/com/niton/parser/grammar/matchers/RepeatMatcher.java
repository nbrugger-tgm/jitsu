package com.niton.parser.grammar.matchers;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.ast.ListNode;
import com.niton.parser.token.TokenStream;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Checks the grammar as often as is ocures
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class RepeatMatcher extends GrammarMatcher<ListNode> {

	private Grammar<?,?>          check;
	public RepeatMatcher(Grammar<?,?> expression) {
		this.check = expression;
	}


	/**
	 * @see GrammarMatcher#process(TokenStream, GrammarReference)
	 * @param tokens
	 * @param ref
	 */
	@Override
	public @NotNull ListNode process(@NotNull TokenStream tokens, @NotNull GrammarReference ref)
	throws ParsingException {
		boolean          keep      = true;
		ListNode         obj       = new ListNode();
		while (keep) {
			try {
				AstNode gr = check.parse(tokens, ref);
				obj.add(gr);
			} catch (ParsingException e) {
				obj.setParsingExceptions(e);
				keep = false;
			}
		}
		return obj;
	}
}
