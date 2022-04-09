package com.niton.parser.grammar;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * The default implementation of a GrammarReference using a {@link HashMap}
 *
 * @author Nils Brugger
 * @version 2019-06-07
 */
public class GrammarReferenceMap extends HashMap<String, Grammar<?,?>>
		implements Iterable<Map.Entry<String, Grammar<?,?>>>, GrammarReference {
	private static final long serialVersionUID = 4311654136057396994L;

	@Override
	public Grammar<?, ?> get(String key) {
		return super.get(key);
	}

	public GrammarReferenceMap map(Grammar<?,?> g) {
		put(g.getName(), g);
		return this;
	}
	public GrammarReferenceMap map(ChainGrammarBuilder g) {
		return map(g.get());
	}

	/**
	 * Adds a Grammar to the reference map
	 *
	 * @param g    the grammar to the map
	 * @param name the name of the grammar
	 */
	public GrammarReferenceMap map(Grammar<?,?> g, String name) {
		put(name, g);
		return this;
	}

	public GrammarReferenceMap merge(GrammarReferenceMap ref) {
		putAll(ref);
		return this;
	}

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Entry<String, Grammar<?,?>>> iterator() {
		return this.entrySet().iterator();
	}

	/**
	 * @see GrammarReference#grammarNames()
	 */
	@Override
	public Set<String> grammarNames() {
		return keySet();
	}

	public GrammarReferenceMap deepMap(ChainGrammarBuilder gram) {
		return deepMap(gram.get());
	}
	public GrammarReferenceMap deepMap(Grammar<?, ?> gram) {
		if (gram.getName() != null && !(gram instanceof GrammarReferenceGrammar)) {
			map(gram);
		}
		if (gram instanceof ChainGrammar) {
			for (Grammar<?, ?> grammar : ((ChainGrammar) gram).getChain()) {
				deepMap(grammar);
			}
		} else if (gram instanceof AnyExceptGrammar) {
			deepMap(((AnyExceptGrammar) gram).getExcept());
		} else if (gram instanceof GrammarReferenceGrammar) {
			//maybe add a mapping if needed
		} else if (gram instanceof IgnoreGrammar) {
			deepMap(((IgnoreGrammar) gram).getGrammar());
		} else if (gram instanceof MultiGrammar) {
			for (Grammar<?,?> grammar : ((MultiGrammar) gram).getGrammars()) {
				deepMap(grammar);
			}
		} else if (gram instanceof OptionalGrammar) {
			deepMap(((OptionalGrammar) gram).getCheck());
		} else if (gram instanceof RepeatGrammar) {
			deepMap(((RepeatGrammar) gram).getCheck());
		}


		return this;
	}
}

