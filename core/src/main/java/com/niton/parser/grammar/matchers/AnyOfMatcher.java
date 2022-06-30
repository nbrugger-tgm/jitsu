package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AnyNode;
import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.MultiGrammar;
import com.niton.parser.token.TokenStream;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * Checks against all given Grammars syncron and returns the first matching
 *
 * @author Nils
 * @version 2019-05-29
 */
public class AnyOfMatcher extends GrammarMatcher<AnyNode> {
	private MultiGrammar grammars;

	public AnyOfMatcher(MultiGrammar grammers) {
		this.grammars = grammers;
	}

	/**
	 * @param tokens
	 * @param ref
	 *
	 * @see GrammarMatcher#process(TokenStream, GrammarReference)
	 */
	@Override
	public @NotNull AnyNode process(@NotNull TokenStream tokens, @NotNull GrammarReference ref)
			throws ParsingException {
		Map<String, Exception> fails = new HashMap<>();
		for (var grammar : this.grammars.getGrammars()) {
			try {
				AstNode obj     = grammar.parse(tokens, ref);
				AnyNode wrapper = new AnyNode(obj);
				wrapper.setType(grammar.getName());
				wrapper.setParsingExceptions(obj.getParsingExceptions());
				return wrapper;
			} catch (ParsingException e) {
				fails.put(grammar.toString(), e);
			}
		}
		throw new ParsingException(String.format(
				"Expected one of : [%s] but none of them was parsable due to  (at: %s): %n%s",
				Arrays.stream(this.grammars.getGrammars()).map(e -> {
					String n;
					if ((n = e.getName()) != null) return n;
					return e.getClass().getSimpleName();
				}).collect(joining(", ")),
				tokens.hasNext()?tokens.get(tokens.index()).getStart():"EOF",
				formatFails(fails)
		));
	}

	private String formatFails(Map<String, Exception> fails) {
		return fails.entrySet()
		            .stream()
		            .map(e -> format(
				            "\t%s --> %s",
				            e.getKey(),
				            Arrays.stream(e.getValue().getMessage().split("\n"))
				                  .map(l -> "\t" + l)
				                  .collect(joining("\n"))
		            )).collect(joining("\n"));
	}

	/**
	 * @return the tokens
	 */
	public Grammar<?, ?>[] getGrammars() {
		return grammars.getGrammars();
	}

	public void setGrammars(MultiGrammar grammars) {
		this.grammars = grammars;
	}
}
