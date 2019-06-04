package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammerObject;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the OptinalGrammer Class
 * @author Nils
 * @version 2019-05-29
 */
public class OptinalGrammer extends Grammer {
    private Grammer check;

    public OptinalGrammer(Grammer value) {
	this.check = value;
    }

    /**
     * @see com.niton.parser.check.Grammer#process(java.util.ArrayList)
     */
    @Override
    public GrammerObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
	try {
	    GrammerObject obj = check.check(tokens, index());
	    if(obj == null)
		throw new ParsingException("");
	    index(check.index());
	    return obj;
	} catch (Exception e) {
	    return new IgnoredGrammerObject();
	}
    }
}

