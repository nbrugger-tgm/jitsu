package com.niton.parser.grammar;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarName;
import com.niton.parser.grammar.types.*;
import com.niton.parser.token.TokenReference;
import com.niton.parser.token.Tokenable;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

public class ChainGrammarBuilder {
	private final ChainGrammar chain           = new ChainGrammar();
	@Getter
	@Setter
	private       boolean      directRecursion = true;

	public ChainGrammarBuilder(String name) {
		chain.setName(name);
	}

	private interface GrammarConverter<T> {
		Grammar<?, ?> toGrammar(T s);
	}

	/**
	 * The matcher provides all the adding methodes for each grammar.
	 * So it is provided for tokens strings and grammars each. And it is not neccessary to add 3
	 * methodes
	 */
	public class RuleApplier {
		private Grammar<?, ?> g;

		public <T> RuleApplier(T s, GrammarConverter<T> converter) {
			g = converter.toGrammar(s);
		}

		public RuleApplier(Grammar<?, ?> t) {
			g = t;
		}

		/**
		 * @see IgnoreGrammar
		 */
		public RuleApplier ignore() {
			g = new IgnoreGrammar(g);
			return this;
		}

		/**
		 * @see RepeatGrammar
		 */
		public RuleApplier repeat() {
			g = new RepeatGrammar(g);
			return this;
		}

		/**
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

		public ChainGrammarBuilder add() {
			chain.addGrammar(g);
			return ChainGrammarBuilder.this;
		}

		public ChainGrammarBuilder add(String message) {
			return name(message);
		}

		/**
		 * Names and adds the grammar. This makes it possible to fetch the match later on
		 *
		 * @param name the name to give
		 */
		public synchronized ChainGrammarBuilder name(String name) {
			chain.addGrammar(g, name);
			return ChainGrammarBuilder.this;
		}
	}


	public class MultiRuleApplier extends RuleApplier {
		public <T> MultiRuleApplier(GrammarConverter<T> converter, T... s) {
			this(Arrays.stream(s).map(converter::toGrammar).toArray(i -> new Grammar<?, ?>[i]));
		}

		public MultiRuleApplier(Grammar<?, ?>... t) {
			super(new MultiGrammar(t));
		}
	}

	//This classes convert The specific types into grammars
	private static class ReferenceGrammarConverter implements GrammarConverter<String> {
		@Override
		public Grammar<?, ?> toGrammar(String s) {
			return new GrammarReferenceGrammar(s);
		}
	}

	//This classes convert The specific types into grammars
	private static class NamedGrammarConverter implements GrammarConverter<GrammarName> {
		@Override
		public Grammar<?, ?> toGrammar(GrammarName s) {
			return new GrammarReferenceGrammar(s.getName());
		}
	}

	private static class TokenGrammarConverter implements GrammarConverter<Tokenable> {
		public Grammar<?, ?> toGrammar(Tokenable s) {
			return new TokenGrammar(s.name());
		}
	}

	private static class TokenNameGrammarConverter implements GrammarConverter<String> {
		public Grammar<?, ?> toGrammar(String s) {
			return new TokenGrammar(s);
		}
	}

	private static class EnumGrammarConverter implements GrammarConverter<Enum<?>> {
		@Override
		public Grammar<?, ?> toGrammar(Enum<?> s) {
			return new GrammarReferenceGrammar(s.name());
		}
	}

	public RuleApplier grammar(GrammarName n) {
		return grammar(n.getName());
	}

	public RuleApplier grammar(String g) {
		if (!directRecursion && chain.getName().equals(g)) {
			throw new IllegalArgumentException("directRecursion forbidden! (" + g + " grammar in " + g + " grammar)\nThis can be enabled using 'setDirectRecursion(boolean)'");
		}
		return new RuleApplier(g, new ReferenceGrammarConverter());
	}

	public RuleApplier grammar(Grammar<?, ?> g) {
		return new RuleApplier(g);
	}

	public RuleApplier token(Tokenable g) {
		return new RuleApplier(g, new TokenGrammarConverter());
	}

	public RuleApplier token(TokenReference g) {
		return token(g.getName());
	}

	public RuleApplier token(String g) {
		return new RuleApplier(g, new TokenNameGrammarConverter());
	}

	public RuleApplier grammars(Grammar<?, ?>... g) {
		return new MultiRuleApplier(g);
	}

	public RuleApplier grammars(String... g) {
		for (String s : g) {
			if (s.equals(chain.getName()) && !directRecursion) {
				throw new IllegalArgumentException(String.format(
						"directRecursion forbidden! (%s grammar in %s grammar)\n" +
								"This can be enabled using 'setDirectRecursion(boolean)'",
						chain.getName(),
						chain.getName()
				));
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

	public RuleApplier keyword(String keyword) {
		return new RuleApplier(Grammar.keyword(keyword));
	}

	public ChainGrammar get() {
		return chain;
	}
}
