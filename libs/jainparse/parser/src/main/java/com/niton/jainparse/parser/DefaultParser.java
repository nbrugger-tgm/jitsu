package com.niton.jainparse.parser;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.token.Tokenable;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * The degault implementation of the parser returning the plain {@link AstNode}
 *
 * @author Nils Brugger
 * @version 2019-06-12
 */
public class DefaultParser<T extends Enum<T> & Tokenable> extends Parser<AstNode<T>, T> {

	public DefaultParser(GrammarReference<T> reference, Grammar<?,T> root) {
		super(reference, root);
	}

	public <G extends Grammar<?,T> & GrammarReference<T>>DefaultParser(G root) {
		super(root, root);
	}

	public DefaultParser(GrammarReference<T> ref, String root) {
		super(ref, root);
	}


	/**
	 * @see Parser#convert(AstNode)
	 */
	@Override
	public @NotNull AstNode<T> convert(@NonNull AstNode<T> o) {
		return o;
	}
}

