package com.niton.jainparse.parser;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarReference;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * The degault implementation of the parser returning the plain {@link AstNode}
 *
 * @author Nils Brugger
 * @version 2019-06-12
 */
public class DefaultParser extends Parser<AstNode> {

	public DefaultParser(GrammarReference reference, Grammar<?> root) {
		super(reference, root);
	}

	public <T extends Grammar<?> & GrammarReference>DefaultParser(T root) {
		super(root, root);
	}

	public DefaultParser(GrammarReference ref, String root) {
		super(ref, root);
	}


	/**
	 * @see Parser#convert(AstNode)
	 */
	@Override
	public @NotNull AstNode convert(@NonNull AstNode o) {
		return o;
	}
}

