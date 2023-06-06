package com.niton.parser;

import com.niton.parser.ast.AstNode;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.ChainGrammar;
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
	 * @see com.niton.parser.Parser#convert(AstNode)
	 */
	@Override
	public @NotNull AstNode convert(@NonNull AstNode o) {
		return o;
	}
}

