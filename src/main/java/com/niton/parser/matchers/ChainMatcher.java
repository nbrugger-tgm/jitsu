package com.niton.parser.matchers;

import com.niton.parser.Grammar;
import com.niton.parser.GrammarMatcher;
import com.niton.parser.GrammarReference;
import com.niton.parser.GrammarResult;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammars.ChainGrammar;
import com.niton.parser.result.SuperGrammarResult;
import com.niton.parser.token.TokenStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Used to build a grammar<br>
 * Tests all Grammars in the chain after each other
 *
 * @author Nils
 * @version 2019-05-28
 */
public class ChainMatcher extends GrammarMatcher<SuperGrammarResult> {

	ChainGrammar chain;
	private ParsingException interrupt;

	/**
	 * Creates an Instance of ChainExecutor.java
	 *
	 * @param chain
	 * @author Nils Brugger
	 * @version 2019-06-08
	 */
	public ChainMatcher(ChainGrammar chain) {
		this.chain = chain;
		setOriginGrammarName(chain.getName());
	}

	@Override
	public ParsingException getLastException() {
		return interrupt;
	}

	/**
	 * @throws ParsingException
	 * @see GrammarMatcher (java.util.List,GrammarReference)
	 */
	@Override
	public SuperGrammarResult process(TokenStream tokens, GrammarReference reference)
	throws ParsingException {
		SuperGrammarResult     gObject    = new SuperGrammarResult();
		Map<String, Exception> exitStates = new HashMap<>();
		int                    i          = 0;
		for (Grammar grammar : getChain()) {
			try {

				GrammarResult res = grammar.parse(tokens, reference);
				String        name;
                if ((name = chain.getNaming().get(i)) != null) {
                    gObject.name(name, res);
                } else {
                    gObject.add(res);
                }
				tokens.commit();
				exitStates.put(grammar.toString(), grammar.getMatcher().getLastException());
				i++;
			} catch (ParsingException e) {
				this.interrupt = new ParsingException(String.format(
						"Chain entry '%s' of '%s' can't be parsed :\n %s",
						grammar,
						chain,
						Arrays.stream(
								e.getMessage().split("\n"))
						      .map(l -> "\t" + l)
						      .collect(Collectors.joining("\n"))), e);
				tokens.rollback();
				throw interrupt;
			}
			interrupt = new ParsingException(
					String.format("'%s' chain elements ended : \n%s",
					              chain,
					              exitStates.entrySet().stream().map(
							              e -> "\t" + e.getKey() + " -> " + (e.getValue() == null ? "no fail" : Arrays
									              .stream(
											              e.getValue().getMessage().split("\n"))
									              .map(l -> "\t" + l)
									              .collect(Collectors.joining("\n")))).collect(
							              Collectors.joining("\n"))));
		}
		return gObject;
	}

	/**
	 * @return the chain
	 */
	public List<Grammar> getChain() {
		return chain.getChain();
	}
}
