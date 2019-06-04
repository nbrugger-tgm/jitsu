package com.niton.parser.check;

import java.util.ArrayList;
import java.util.Arrays;

import com.niton.parser.GrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the MultiTokenGrammer Class
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class MultiGrammer extends Grammer {
    private Grammer[] tokens;

    public MultiGrammer(Grammer[] tokens) {
	this.tokens = tokens;
    }

    /**
     * @see com.niton.parser.check.Grammer#process(java.util.ArrayList)
     */
    @Override
    public GrammerObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
	for (Grammer g : this.tokens) {
	    try {
		GrammerObject obj = g.check(tokens, index());
		if(obj == null)
		    throw new ParsingException("");
		index(g.index());
		return obj;
	    } catch (ParsingException e) {
	    }
	}
	throw new ParsingException("Expected Grammer (OR) : "+Arrays.toString(this.tokens)+" but none of them was parsable");
    }
}
