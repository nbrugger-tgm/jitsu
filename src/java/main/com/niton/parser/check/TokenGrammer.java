package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the TokenGrammer Class
 * @author Nils
 * @version 2019-05-28
 */
public class TokenGrammer extends Grammer {
    private String tokenName;
    

    /**
     * Creates an Instance of TokenGrammer.java
     * @author Nils
     * @version 2019-05-28
     * @param tokenName
     */
    public TokenGrammer(String tokenName) {
	super();
	this.tokenName = tokenName;
    }

    /**
     * @throws ParsingException 
     * @see com.niton.parser.check.Grammer#check(java.util.ArrayList)
     */
    @Override
    public GrammerObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
	AssignedToken token = tokens.get(index());
	if(token.name.equals(tokenName)) {
	    TokenGrammerObject obj = new TokenGrammerObject();
	    obj.tokens.add(token);
	    increase();
	    return obj;
	}
	throw new ParsingException("Expected Token : "+tokenName+" but actual value was : "+token.name);
    }

    /**
     * @return the tokenName
     */
    public String getTokenName() {
	return tokenName;
    }

    /**
     * @param tokenName the tokenName to set
     */
    public void setTokenName(String tokenName) {
	this.tokenName = tokenName;
    }
}

