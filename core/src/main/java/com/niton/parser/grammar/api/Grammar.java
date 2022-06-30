package com.niton.parser.grammar.api;

import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.ChainGrammarBuilder;
import com.niton.parser.grammar.GrammarReferenceMap;
import com.niton.parser.grammar.types.*;
import com.niton.parser.token.TokenReference;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenable;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * A Grammar is a rule how to collect tokens together and handle them
 *
 * @author Nils
 * @version 2019-05-28
 */
public abstract class Grammar<M extends GrammarMatcher<R>, R extends AstNode> {
	private String name;

	public static ChainGrammarBuilder build(GrammarName string) {
		return build(string.getName());
	}

	public static ChainGrammarBuilder build(String string) {
		return new ChainGrammarBuilder(string);
	}

	public static AnyExceptGrammar anyExcept(Grammar<?, ?> except) {
		return new AnyExceptGrammar(except);
	}

	public static GrammarReferenceGrammar reference(String name) {
		return new GrammarReferenceGrammar(name);
	}

	public static GrammarReferenceGrammar reference(GrammarName name) {
		return new GrammarReferenceGrammar(name.getName());
	}

	public static IgnoreGrammar ignore(Grammar<?, ?> g) {
		return new IgnoreGrammar(g);
	}

	public static MultiGrammar anyOf(Grammar<?, ?>... grammars) {
		return new MultiGrammar(grammars);
	}

	public static OptionalGrammar optional(Grammar<?, ?> g) {
		return new OptionalGrammar(g);
	}

	public static RepeatGrammar repeat(Grammar<?, ?> g) {
		return new RepeatGrammar(g);
	}

	public static TokenGrammar tokenReference(TokenReference token) {
		return tokenReference(token.getName());
	}

	public static TokenGrammar tokenReference(String token) {
		return new TokenGrammar(token);
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

	public void map(GrammarReferenceMap ref) {
		ref.map(this);
	}

	public boolean parsable(@NonNull TokenStream tokens,@NonNull GrammarReference ref) {
		tokens.elevate();
		try {
			M matcher = createExecutor();
			matcher.setOriginGrammarName(getName());
			matcher.parse(tokens, ref);
			tokens.rollback();
			return true;
		} catch (ParsingException pex) {
			tokens.rollback();
			return false;
		}
	}

	/**
	 * To see what a executor is look at {@link GrammarMatcher}
	 */
	protected abstract M createExecutor();

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 *
	 * @return
	 */
	public Grammar<M, R> setName(String name) {
		this.name = name;
		return this;
	}

	@NotNull
	public R parse(@NonNull TokenStream tokens, @NotNull GrammarReference ref)
			throws ParsingException {
		M matcher = createExecutor();
		matcher.setOriginGrammarName(getName());
		return matcher.parse(tokens, ref);
	}

	public abstract void reconfigMatcher(@NonNull M m);

	@Override
	public String toString() {
		return String.format("%s(%s)", this.getClass().getSimpleName(), getName());
	}

	public MultiGrammar or(Grammar<?, ?>... alternatives) {
		var combined = new Grammar<?,?>[alternatives.length+1];
		combined[0] = this;
		System.arraycopy(alternatives,0,combined,1,alternatives.length);
		return new MultiGrammar(combined);
	}

	public ChainGrammar then(Grammar<?, ?> tokenDefiner) {
		var chain = new ChainGrammar();
		chain.addGrammar(this);
		chain.addGrammar(tokenDefiner);
		return chain;
	}

	public Grammar<M,R> named(String name){
		this.name = name;
		return this;
	}

	public Grammar<M,R> named(GrammarName name){
		this.name = name.getName();
		return this;
	}

}

