package com.niton.parser.matchers;

import com.niton.parser.Grammar;
import com.niton.parser.GrammarMatcher;
import com.niton.parser.GrammarReference;
import com.niton.parser.GrammarResult;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammars.MultiGrammar;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.token.TokenStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks against all given Grammars syncron and returns the first matching
 *
 * @author Nils
 * @version 2019-05-29
 */
public class AnyOfMatcher extends GrammarMatcher<AnyGrammarResult> {

	private ParsingException interrupt;
	private MultiGrammar grammars;

	public AnyOfMatcher(MultiGrammar grammers) {
		this.grammars = grammers;
	}

	@Override
	public ParsingException getLastException() {
		return interrupt;
	}

	/**
	 * @see GrammarMatcher#process(TokenStream, GrammarReference)
	 */
	@Override
	public AnyGrammarResult process(TokenStream tokens, GrammarReference ref)
	throws ParsingException {
		Map<String, Exception> fails = new HashMap<>();
		for (Grammar grammar : this.grammars.getGrammars()) {
			try {
				GrammarResult obj = grammar.parse(tokens, ref);
                if (obj == null) {
                    throw new ParsingException("Tokens do not match subgrammar " + grammar.getName());
                }
				AnyGrammarResult wrapper = new AnyGrammarResult(obj);
				wrapper.setType(grammar.getName());
				tokens.commit();
				return wrapper;
			} catch (ParsingException e) {
				fails.put(grammar.toString(), e);
				tokens.rollback();
			}
		}
		throw interrupt = new ParsingException(
				"Expected Grammar (OR) : [" +
						Arrays.stream(this.grammars.getGrammars()).map(e -> {
							String n;
							if ((n = e.getName()) != null) return n;
							return e.getClass().getSimpleName();
						}).collect(Collectors.joining(", ")) +
						"] but none of them was parsable due to : \n" +
						fails.entrySet()
						     .stream()
						     .map(e -> String.format("\t%s --> %s",
						                             e.getKey(),
						                             e.getValue() == null ? "success" :
								                             Arrays.stream(
										                             e.getValue()
										                              .getMessage()
										                              .split("\n"))
								                                   .map(l -> "\t" + l)
								                                   .collect(Collectors.joining(
										                                   "\n"))))
						     .collect(Collectors.joining("\n")));
	}

	/**
	 * @return the tokens
	 */
	public Grammar[] getGrammars() {
		return grammars.getGrammars();
	}

	public void setGrammars(MultiGrammar grammars) {
		this.grammars = grammars;
	}
}
