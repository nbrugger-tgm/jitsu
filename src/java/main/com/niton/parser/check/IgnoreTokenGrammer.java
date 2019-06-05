package com.niton.parser.check;

import java.util.ArrayList;
import java.util.Iterator;

import com.niton.parser.GrammarObject;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Ignores the token and adds empty elements to the object
 * @author Nils
 * @version 2019-05-29
 */
public class IgnoreTokenGrammer extends Grammar {
    private String token;

    public IgnoreTokenGrammer(String name2) {
	this.token = name2;
    }

    /**
     * @see com.niton.parser.check.Grammar#process(java.util.ArrayList)
     */
    @Override
    public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
	Iterator<AssignedToken> ts = tokens.listIterator(index());
	while(ts.hasNext() && ts.next().name.equals(token))
	    increase();
	return new IgnoredGrammerObject();
    }

	/**
	 * @see com.niton.parser.check.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return IgnoredGrammerObject.class;
	}

}

