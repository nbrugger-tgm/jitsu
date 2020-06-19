package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;
import com.niton.parser.grammar.Grammar;

/**
 * Used to build a grammar<br>
 * Tests all Grammars in the chain after each other
 * 
 * @author Nils
 * @version 2019-05-28
 */
public class ChainExecutor extends GrammarExecutor {
	
	ArrayList<Grammar> chain = new ArrayList<>();

	/**
	 * Creates an Instance of ChainExecutor.java
	 * @author Nils Brugger
	 * @version 2019-06-08
	 * @param chain2
	 */
	public ChainExecutor(ArrayList<Grammar> chain2) {
		this.chain = chain2;
	}
	/**
	 * @throws ParsingException
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList,GrammarReference)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens,GrammarReference reference) throws ParsingException {
		SubGrammerObject gObject = new SubGrammerObject();
		gObject.setName(getName());
		for (Grammar grammer : chain) {

			GrammarExecutor g = grammer.getExecutor();
			gObject.objects.add(g.check(tokens, index(),reference));
			index(g.index());
		}
		return gObject;
	}
	/**
	 * @return the chain
	 */
	public ArrayList<Grammar> getChain() {
		return chain;
	}
}
