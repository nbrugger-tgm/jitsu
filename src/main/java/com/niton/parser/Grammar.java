package com.niton.parser;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammars.*;
import com.niton.parser.token.TokenStream;

/**
 * A Grammar is a rule how to collect tokens together and handle them
 *
 * @author Nils
 * @version 2019-05-28
 */
public abstract class Grammar<M extends GrammarMatcher<R>, R extends GrammarResult> {
	private String name;
	private M      matcher = createExecutor();


	public static ChainGrammar build(String string) {
		return (ChainGrammar) new ChainGrammar().setName(string);
	}

	public static ChainGrammar build(GrammarName string) {
		return (ChainGrammar) new ChainGrammar().setName(string.getName());
	}

	public static AnyExceptGrammar anyExcept(Grammar except) {
		return new AnyExceptGrammar(except);
	}

	public static GrammarReferenceGrammar reference(String name) {
		return new GrammarReferenceGrammar(name);
	}

	public static IgnoreGrammar ignore(Grammar g) {
		return new IgnoreGrammar(g);
	}

	public static MultiGrammar anyOf(Grammar... grammars) {
		return new MultiGrammar(grammars);
	}

	public static OptionalGrammar optional(Grammar g) {
		return new OptionalGrammar(g);
	}

	public static RepeatGrammar repeat(Grammar g) {
		return new RepeatGrammar(g);
	}

	public static TokenGrammar tokenReference(String token) {
		return new TokenGrammar(token);
	}

	public static TokenGrammar tokenReference(TokenReference token) {
		return tokenReference(token.getName());
	}

	public static TokenGrammar tokenReference(Tokenable token) {
		return tokenReference(token.name());
	}

	public AnyExceptGrammar anyExcept() {
		return new AnyExceptGrammar(this);
	}

	public IgnoreGrammar ignore() {
		return new IgnoreGrammar(this);
	}

	public OptionalGrammar optional() {
		return new OptionalGrammar(this);
	}

	public RepeatGrammar repeat() {
		return new RepeatGrammar(this);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @return
	 */
	public Grammar setName(String name) {
		this.name = name;
		return this;
	}

	public void map(GrammarReferenceMap ref) {
		ref.map(this);
	}

	/**
	 * To see what a executor is look at {@link GrammarMatcher}
	 *
	 * @return
	 */
	protected abstract M createExecutor();

	public M getMatcher() {
		reconfigMatcher(matcher);
		return matcher;
	}

	public boolean parsable(TokenStream tokens, GrammarReference ref) {
		try {
			M matcher = getMatcher();
			matcher.setOriginGrammarName(getName());
			matcher.parse(tokens, ref);
			tokens.rollback();
			return true;
		} catch (ParsingException pex) {
			tokens.rollback();
			return false;
		}
	}

	public R parse(TokenStream tokens, GrammarReference ref) throws ParsingException {
		M matcher = getMatcher();
		matcher.setOriginGrammarName(getName());
		return matcher.parse(tokens, ref);
	}

	public abstract void reconfigMatcher(M m);

	@Override
	public String toString() {
		return String.format("%s(%s)", this.getClass().getSimpleName(), getName());
	}
}

