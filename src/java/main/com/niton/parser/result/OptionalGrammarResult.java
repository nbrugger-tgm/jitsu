package com.niton.parser.result;

import com.niton.parser.GrammarResult;
import com.niton.parser.token.Tokenizer;

import java.util.Collection;
import java.util.LinkedList;

/**
 * This is the IngoredGrammarObject Class
 * @author Nils
 * @version 2019-05-29
 */
public class OptionalGrammarResult extends GrammarResult {
    private GrammarResult value = null;
    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "[optional]";
    }

    public void setValue(GrammarResult value) {
        this.value = value;
    }

    public GrammarResult getValue() {
        return value;
    }

    @Override
    public Collection<? extends Tokenizer.AssignedToken> join() {
        return value != null ? value.join() : new LinkedList<>();
    }
}

