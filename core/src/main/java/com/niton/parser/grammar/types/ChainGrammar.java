package com.niton.parser.grammar.types;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.matchers.ChainMatcher;
import com.niton.parser.ast.SequenceNode;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Used to build a grammar<br>
 * Tests all Grammars in the chain after each other
 *
 * @author Nils
 * @version 2019-05-28
 */
public class ChainGrammar extends Grammar<SequenceNode>
		implements GrammarReference {
	private final List<Grammar<?>>        chain           = new LinkedList<>();
	@Getter
	private final Map<Integer, String> naming          = new HashMap<>();

	public List<Grammar<?>> getChain() {
		return chain;
	}
	public void addGrammar(Grammar<?> grammar) {
		chain.add(grammar);
	}
	public void addGrammar(Grammar<?> grammar, String name) {
		chain.add(grammar);
		naming.put(chain.size()-1,name);
	}

	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public ChainMatcher createExecutor() {
		return new ChainMatcher(this);
	}

    @Override
	public Grammar<?> get(String key) {
		if (getName().equals(key)) return this;
		for (Grammar<?> g : chain) {
			if (g.getName().equals(key)) {
				return g;
			}
		}
		return null;
	}

	/**
	 * Returns the names of all the grammars in the chain including itself
	 */
	@Override
	public Set<String> grammarNames() {
		Set<String> s = chain.stream()
		                     .map(Grammar::getName)
		                     .filter(Objects::nonNull)
		                     .collect(Collectors.toSet());
		s.add(this.getName());
		return s;
	}

	@Override
	public ChainGrammar then(Grammar<?> tokenDefiner) {
		addGrammar(tokenDefiner);
		return this;
	}
}
