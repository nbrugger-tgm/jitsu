package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the Grammer Class
 * @author Nils
 * @version 2019-05-28
 */
public abstract class Grammer {
    private int index;
    /**
     * Description : 
     * @author Nils
     * @version 2019-05-28
     * @throws ParsingException 
     */
    public final GrammerObject check(ArrayList<AssignedToken> tokens) throws ParsingException {
	index = 0;
	return process(tokens);
    }
    
    public static ChainGrammer build(String string) {
	return new ChainGrammer().name(string);
    }

    /**
     * Description : 
     * @author Nils
     * @version 2019-05-29
     * @param tokens
     * @param pos
     * @return
     * @throws ParsingException 
     */
    public final GrammerObject check(ArrayList<AssignedToken> tokens, int pos) throws ParsingException {
	if(index >= tokens.size())
	    throw new ParsingException("No More Tokens!");
	index = pos;
	return process(tokens);
    }
    
    public abstract GrammerObject process(ArrayList<AssignedToken> tokens) throws ParsingException;
    
    protected final void increase() {
	index++;
    }
    /**
     * @return the index
     */
    public int index() {
	return index;
    }
    /**
     * @return the index
     */
    public void index(int index) {
	this.index= index;
    }
}

