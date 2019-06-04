package com.niton.parser.check;

import java.util.ArrayList;
import java.util.Iterator;

import com.niton.parser.GrammerObject;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the IgnoreTokenGrammer Class
 * @author Nils
 * @version 2019-05-29
 */
public class IgnoreGrammer extends Grammer {
    private Grammer token;

    public IgnoreGrammer(Grammer name2) {
	this.token = name2;
    }

    /**
     * @see com.niton.parser.check.Grammer#process(java.util.ArrayList)
     */
    @Override
    public GrammerObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
	try {
	    token.check(tokens, index());
	    index(token.index());
	} catch (ParsingException e) {
	}
	return new IgnoredGrammerObject();
    }
}

