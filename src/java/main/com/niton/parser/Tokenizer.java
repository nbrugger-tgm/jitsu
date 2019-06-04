package com.niton.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the Parser Class
 * @author Nils
 * @version 2019-05-27
 */
public class Tokenizer {
    public HashMap<String,Token> tokens = new HashMap<>();
    
    public class AssignedToken{
	public String value;
	public Token token;
	public String name;
	public int start;
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
	    return value+"("+name+")";
	}
    }
    
    public ArrayList<AssignedToken> parse(String content){
	ArrayList<AssignedToken> tokens = new ArrayList<>();
	for (String tokenName : this.tokens.keySet()) {
	    Token t = this.tokens.get(tokenName);
	    Pattern p = t.getCompletePattern();
	    Matcher m = p.matcher(content);
	    while(m.find()) {
		AssignedToken res = new AssignedToken();
		res.name = tokenName;
		res.token = t;
		res.value = m.group();
		res.start = m.start();
		tokens.add(res);
	    }
	}
	tokens.sort((AssignedToken o1, AssignedToken o2) -> {
		return o1.start-o2.start;
	    }
	);
	return tokens;
    }

    /**
     * Creates an Instance of Tokenizer.java
     * @author Nils
     * @version 2019-05-28
     */
    public Tokenizer() {
	for (Tokens t : Tokens.values()) {
	    tokens.put(t.name(), new Token(t.pattern));
	}
    }

}

