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
public class MultiTokenGrammer extends Grammer {
    private String[] tokens;

    public MultiTokenGrammer(String[] tokens) {
	this.tokens = tokens;
    }

    /**
     * @see com.niton.parser.check.Grammer#process(java.util.ArrayList)
     */
    @Override
    public GrammerObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
	AssignedToken token = tokens.get(index());
	for(int i = 0;i<this.tokens.length;i++) {
        	if(token.name.equals(this.tokens[i])) {
        	    TokenGrammerObject obj = new TokenGrammerObject();
        	    obj.tokens.add(token);
        	    increase();
        	    return obj;
        	}
	}
	throw new ParsingException("Expected Tokens (OR) : "+Arrays.toString(this.tokens)+" but actual value was : "+token.name);
    }
}
