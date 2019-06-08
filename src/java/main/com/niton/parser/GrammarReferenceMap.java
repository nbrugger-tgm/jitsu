package com.niton.parser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.niton.parser.grammar.Grammar;

/**
 * This is the GrammerReference Class
 * @author Nils Brugger
 * @version 2019-06-07
 */
public class GrammarReferenceMap extends HashMap<String, Grammar> implements Iterable<Map.Entry<String, Grammar>>, GrammarReference {
	/**
	 * <b>Type:</b> long<br> 
	 * <b>Description:</b><br>
	 */
	private static final long serialVersionUID = 4311654136057396994L;
	
	@Override
	public Grammar get(String key) {
		return super.get(key);
	}
	public GrammarReferenceMap map(Grammar g) {
		put(g.getName(), g);
		return this;
	}

	public GrammarReferenceMap map(Grammar g,String name) {
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
	public Iterator<Entry<String, Grammar>> iterator() {
		return this.entrySet().iterator();
	}
}

