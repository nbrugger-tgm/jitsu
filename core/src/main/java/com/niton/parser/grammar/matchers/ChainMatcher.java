package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.SuperNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.ChainGrammar;
import com.niton.parser.token.TokenStream;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Used to build a grammar<br>
 * Tests all Grammars in the chain after each other
 *
 * @author Nils
 * @version 2019-05-28
 */
@Getter
@Setter
public class ChainMatcher extends GrammarMatcher<SuperNode> {

	private ChainGrammar chain;

	/**
	 * Creates an Instance of ChainExecutor.java
	 *
	 * @param chain
	 *
	 * @author Nils Brugger
	 * @version 2019-06-08
	 */
	public ChainMatcher(ChainGrammar chain) {
		this.chain = chain;
		setOriginGrammarName(chain.getName());
	}

	@Override
	public @NotNull SuperNode process(
			@NotNull TokenStream tokens,
			@NotNull GrammarReference reference
	)
			throws ParsingException {
		SuperNode              gObject    = new SuperNode();
		Map<String, Exception> exitStates = new HashMap<>();
		int                    i          = 0;
		for (var grammar : chain.getChain()) {
			try {
				var    res = grammar.parse(tokens, reference);
				String name;
				if ((name = chain.getNaming().get(i++)) != null) {
					gObject.name(name, res);
				} else {
					gObject.add(res);
				}
				exitStates.put(grammar.toString(), res.getParsingExceptions());
			} catch (ParsingException e) {
				throw new ParsingException(format(
						"Chain entry '%s' of '%s' can't be parsed :%n %s",
						grammar,
						chain,
						Arrays.stream(e.getMessage().split("\n"))
						      .map(l -> "\t" + l)
						      .collect(Collectors.joining("\n"))
				), e);
			}
			gObject.setParsingExceptions(new ParsingException(format(
					"'%s' chain elements ended : %n%s",
					chain,
					exitStates.entrySet()
					          .stream()
					          .filter(Objects::nonNull)
					          .map(e -> format(
							          "\t%s -> %s",
							          e.getKey(),
							          e.getValue() == null ? "success" :
									          Arrays.stream(e.getValue().getMessage().split("\n"))
									                .map(l -> "\t" + l)
									                .collect(Collectors.joining("\n"))
					          )).collect(Collectors.joining("\n"))
			)));
		}
		return gObject;
	}


}
