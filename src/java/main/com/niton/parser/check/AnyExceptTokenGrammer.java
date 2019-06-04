package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the AnyExceptTokenGrammer Class
 * @author Nils
 * @version 2019-05-29
 */
public class AnyExceptTokenGrammer extends Grammer {

    private String dunnoaccept;

    public AnyExceptTokenGrammer(String string) {
	this.dunnoaccept = string;
    }

    /**
     * @see com.niton.parser.check.Grammer#process(java.util.ArrayList)
     */
    @Override
    public GrammerObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
	TokenGrammerObject obj = new TokenGrammerObject();
	for (; index() < tokens.size(); increase()) {
	    AssignedToken token = tokens.get(index());
	    if(token.name.equals(dunnoaccept))
		return obj;
	    obj.tokens.add(token);
	}
	return null;
    }
}

