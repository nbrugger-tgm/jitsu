package com.niton.parser.grammars;

import com.niton.parser.*;
import com.niton.parser.matchers.ChainMatcher;
import com.niton.parser.result.SuperGrammarResult;
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
public class ChainGrammar extends Grammar<ChainMatcher, SuperGrammarResult>
		implements GrammarReference {
	private final List<Grammar>        chain           = new LinkedList<>();
	@Getter
	private final Map<Integer, String> naming          = new HashMap<>();
	private       boolean              directRecursion = false;

	public List<Grammar> getChain() {
		return chain;
	}

	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public ChainMatcher createExecutor() {
		return new ChainMatcher(this);
	}

	@Override
	public void reconfigMatcher(ChainMatcher chainMatcher) {
	}

	public RuleApplier grammar(GrammarName n) {
		return grammar(n.getName());
	}

	public RuleApplier grammar(Grammar g) {
		return new RuleApplier(g);
	}

	public RuleApplier grammar(String g) {
		if (!directRecursion && getName().equals(g)) {
			throw new IllegalArgumentException("directRecursion forbidden! (" + g + " grammar in " + g + " grammar)\nThis can be enabled using 'setDirectRecursion(boolean)'");
		}
		return new RuleApplier(g, new ReferenceGrammarConverter());
	}


	public RuleApplier token(Tokenable g) {
		return new RuleApplier(g, new TokenGrammarConverter());
	}

	public RuleApplier token(String g) {
		return new RuleApplier(g, new TokenNameGrammarConverter());
	}

	public RuleApplier token(TokenReference g) {
		return token(g.getName());
	}


	public RuleApplier grammars(Grammar... g) {
		return new MultiRuleApplier(g);
	}

	public RuleApplier grammars(String... g) {
		for (String s : g) {
			if (s.equals(this.getName()) && !directRecursion) {
				throw new IllegalArgumentException("directRecursion forbidden! (" + g + " grammar in " + g + " grammar)\nThis can be enabled using 'setDirectRecursion(boolean)'");
			}
		}

		return new MultiRuleApplier(new ReferenceGrammarConverter(), g);
	}

	public RuleApplier grammars(GrammarName... g) {
		return new MultiRuleApplier(new NamedGrammarConverter(), g);
	}


	public RuleApplier tokens(Tokenable... g) {
		return new MultiRuleApplier(new TokenGrammarConverter(), g);
	}

	public RuleApplier tokens(String... g) {
		return new MultiRuleApplier(new TokenNameGrammarConverter(), g);
	}

	public boolean isDirectRecursion() {
		return directRecursion;
	}

	public void setDirectRecursion(boolean directRecursion) {
		this.directRecursion = directRecursion;
	}

	@Override
	public Grammar get(String key) {
		if (getName().equals(key)) return this;
		for (Grammar g : chain) {
			if (g.getName().equals(key)) {
				return g;
			}
		}
		return null;
	}

	@Override
	/**
	 * Returns the names of all the grammars in the chain including itself
	 */
	public Set<String> grammarNames() {
		Set<String> s = chain.stream()
		                     .map(Grammar::getName)
		                     .filter(e -> e != null)
		                     .collect(Collectors.toSet());
		s.add(this.getName());
		return s;
	}


	private interface GrammarConverter<T> {
		abstract Grammar toGrammar(T s);
	}

	/**
	 * The matcher provides all the adding methodes for each grammar.
	 * So it is provided for tokens strings and grammars each. And it is not neccessary to add 3 methodes
	 */
	public class RuleApplier {
		private Grammar g;

		public <T> RuleApplier(T s, GrammarConverter<T> converter) {
			g = converter.toGrammar(s);
		}

		public RuleApplier(Grammar t) {
			g = t;
		}

		/**
		 * @return
		 * @see IgnoreGrammar
		 */
		public RuleApplier ignore() {
			g = new IgnoreGrammar(g);
			return this;
		}

		/**
		 * @return
		 * @see RepeatGrammar
		 */
		public RuleApplier repeat() {
			g = new RepeatGrammar(g);
			return this;
		}

		/**
		 * @return
		 * @see OptionalGrammar
		 */
		public RuleApplier optional() {
			g = new OptionalGrammar(g);
			return this;
		}

		public RuleApplier anyExcept() {
			g = new AnyExceptGrammar(g);
			return this;
		}

		public ChainGrammar add() {
			chain.add(g);
			return ChainGrammar.this;
		}

		/**
		 * Names and adds the grammar. This makes it possible to fetch the match later on
		 *
		 * @param name the name to give
		 */
		public synchronized ChainGrammar name(String name) {
			add();
			if (name != null) {
				naming.put(chain.size() - 1, name);
				Grammar g;
				if ((g = chain.get(chain.size() - 1)) != null) {
					g.setName(name);
				}
			}
			return ChainGrammar.this;
		}

		public ChainGrammar add(String message) {
			return name(message);
		}
	}


	public class MultiRuleApplier extends RuleApplier {


		public <T> MultiRuleApplier(GrammarConverter<T> converter, T... s) {
			this(Arrays.stream(s).map(converter::toGrammar).toArray(i -> new Grammar[i]));
		}

		public MultiRuleApplier(Grammar... t) {
			super(new MultiGrammar(t));
		}
	}

	//This classes convert The specific types into grammars
	private class ReferenceGrammarConverter implements GrammarConverter<String> {
		@Override
		public Grammar toGrammar(String s) {
			return new GrammarReferenceGrammar(s);
		}
	}

	//This classes convert The specific types into grammars
	private class NamedGrammarConverter implements GrammarConverter<GrammarName> {
		@Override
		public Grammar toGrammar(GrammarName s) {
			return new GrammarReferenceGrammar(s.getName());
		}
	}

	private class TokenGrammarConverter implements GrammarConverter<Tokenable> {
		public Grammar toGrammar(Tokenable s) {
			return new TokenGrammar(s.name());
		}
	}

	private class TokenNameGrammarConverter implements GrammarConverter<String> {
		public Grammar toGrammar(String s) {
			return new TokenGrammar(s);
		}
	}

	private class EnumGrammarConverter implements GrammarConverter<Enum> {
		@Override
		public Grammar toGrammar(Enum s) {
			return new GrammarReferenceGrammar(s.name());
		}
	}
}
