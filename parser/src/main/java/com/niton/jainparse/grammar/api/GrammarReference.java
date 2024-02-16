package com.niton.jainparse.grammar.api;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Used to resolve grammar names.
 * Implementation to build a reference: {@link GrammarReferenceMap}
 *
 * @author Nils Brugger
 * @version 2019-06-07
 */
public interface GrammarReference {
	/**
	 * Should return the grammar associated with the key
	 *
	 * @param key the name of the grammar
	 * @return the grammar or null of no grammar with this name is present in this reference
	 */
	@Nullable Grammar<?> get(String key);

	/**
	 * @return the name of all contained Grammars
	 */
	Set<String> grammarNames();
}

