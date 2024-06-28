package com.niton.jainparse.grammar.api;

import com.niton.jainparse.grammar.types.*;
import com.niton.jainparse.token.Tokenable;
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
public class GrammarReferenceMap<T extends Enum<T> & Tokenable> extends HashMap<String, Grammar<?,T>>
        implements Iterable<Map.Entry<String, Grammar<?,T>>>, GrammarReference<T> {

    @Override
    public Grammar<?,T> get(String key) {
        return super.get(key);
    }

    public GrammarReferenceMap<T> map(Grammar<?,T> g) {
        put(g.getName(), g);
        return this;
    }

    public GrammarReferenceMap<T> map(Grammar.Builder<T> g) {
        return map(g.get());
    }

    /**
     * Adds a Grammar to the reference map
     *
     * @param g    the grammar to the map
     * @param name the name of the grammar
     */
    public GrammarReferenceMap<T> map(Grammar<?,T> g, String name) {
        put(name, g);
        return this;
    }

    public GrammarReferenceMap<T> merge(GrammarReferenceMap<T> ref) {
        putAll(ref);
        return this;
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Entry<String, Grammar<?,T>>> iterator() {
        return this.entrySet().iterator();
    }

    /**
     * @see GrammarReference#grammarNames()
     */
    @Override
    public Set<String> grammarNames() {
        return keySet();
    }

    public GrammarReferenceMap<T> deepMap(@Nullable Grammar<?,T> gram) {
        if (gram == null) return this;
        if (gram.getName() != null && !(gram instanceof GrammarReferenceGrammar)) {
            map(gram);
        }
        if (gram instanceof GrammarReference) {
            var ref = (GrammarReference<T>) gram;
            for (var grammarName : ref.grammarNames()) {
                if (grammarName.equals(gram.getName())) continue;
                Grammar<?,T> grammar = ref.get(grammarName);
                //this if prevents from infinite recursion by linking the reference to the name rather than the actual grammar
                if (isNameMappable(grammar)) map(grammar);
            }
        }

        return this;
    }

    private static boolean isNameMappable(Grammar<?,?> grammar) {
        if(!(grammar instanceof GrammarReferenceGrammar)) return true;
        var referenceGrammar = (GrammarReferenceGrammar<?>) grammar;
        return !grammar.getName().equals(referenceGrammar.getGrammar());
    }
}

