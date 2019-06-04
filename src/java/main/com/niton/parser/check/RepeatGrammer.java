package com.niton.parser.check;

import java.util.ArrayList;

import com.niton.parser.GrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the RepeatGrammer Class
 * @author Nils
 * @version 2019-05-29
 */
public class RepeatGrammer extends Grammer {
    private Grammer check;
    private String name;

    public RepeatGrammer(Grammer expression, String name) {
	this.check = expression;
	this.name = name;
    }

    /**
     * @see com.niton.parser.check.Grammer#process(java.util.ArrayList)
     */
    @Override
    public GrammerObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
	boolean keep = true;
	SubGrammerObject obj = new SubGrammerObject();
	obj.name = name;
	while(keep) {
	    try {
		GrammerObject gr = check.check(tokens, index());
		if(gr == null)
		    throw new ParsingException("");
		obj.objects.add(gr);
		index(check.index());
	    } catch (ParsingException e) {
		return obj;
	    }
	}
	return obj;
    }
}

