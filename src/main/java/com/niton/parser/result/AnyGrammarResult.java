package com.niton.parser.result;

import com.niton.parser.GrammarResult;
import com.niton.parser.grammars.MultiGrammar;
import com.niton.parser.token.Tokenizer;

import java.util.Collection;

/**
 * The result of a {@link MultiGrammar}
 * This is the result if it was not sure which type of grammar was going to be at this position so this is kind of a wildcard but in its parsed form the type is known and accessible
 *
 * @author Nils Brugger
 * @version 2019-06-14
 */
public class AnyGrammarResult extends GrammarResult {
	private final GrammarResult res;
	private       String        type;

	public AnyGrammarResult(GrammarResult res) {
		this.res = res;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public Collection<? extends Tokenizer.AssignedToken> join() {
		return res.join();
	}

	public GrammarResult getRes() {
		return res;
	}

	@Override
	public String toString() {
		return res.toString();
	}
}

