package com.niton.parser.grammar.api;

import com.niton.parser.grammar.types.*;
import org.jetbrains.annotations.Nullable;

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
public class GrammarReferenceMap extends HashMap<String, Grammar<?>>
		implements Iterable<Map.Entry<String, Grammar<?>>>, GrammarReference {

	@Override
	public Grammar<?> get(String key) {
		return super.get(key);
	}

	public GrammarReferenceMap map(Grammar<?> g) {
		put(g.getName(), g);
		return this;
	}
	public GrammarReferenceMap map(Grammar.Builder g) {
		return map(g.get());
	}

	/**
	 * Adds a Grammar to the reference map
	 *
	 * @param g    the grammar to the map
	 * @param name the name of the grammar
	 */
	public GrammarReferenceMap map(Grammar<?> g, String name) {
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
	public Iterator<Entry<String, Grammar<?>>> iterator() {
		return this.entrySet().iterator();
	}

	/**
	 * @see GrammarReference#grammarNames()
	 */
	@Override
	public Set<String> grammarNames() {
		return keySet();
	}

	public GrammarReferenceMap deepMap(Grammar.Builder gram) {
		return deepMap(gram.get());
	}
	public GrammarReferenceMap deepMap(@Nullable Grammar<?> gram) {
		if(gram == null) return this;
		if (gram.getName() != null && !(gram instanceof GrammarReferenceGrammar)) {
			map(gram);
		}
		if (gram instanceof GrammarReference) {
			var ref = (GrammarReference) gram;
			for (var grammar : ref.grammarNames()) {
				deepMap(ref.get(grammar));
			}
		}

		return this;
	}
}

