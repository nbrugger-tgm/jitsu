package com.niton.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the Parser Class
 * 
 * @author Nils
 * @version 2019-05-27
 */
public class Tokenizer {
	public HashMap<String, Token> tokens = new HashMap<>();
	public static final Token UNDIFINED = new Token("");
	public class AssignedToken {
		public String value;
		public Token token;
		public String name;
		public int start;

		public AssignedToken(String value, Token token, String name) {
			super();
			this.value = value;
			this.token = token;
			this.name = name;
		}

		/**
		 * Creates an Instance of Tokenizer.java
		 * 
		 * @author Nils Brugger
		 * @version 2019-06-04
		 */
		public AssignedToken() {
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return value + "(" + name + ")";
		}
	}

	public ArrayList<AssignedToken> parse(String content) throws ParsingException {
		ArrayList<AssignedToken> tokens = new ArrayList<>();
		for (String tokenName : this.tokens.keySet()) {
			Token t = this.tokens.get(tokenName);
			Pattern p = t.getCompletePattern();
			Matcher m = p.matcher(content);
			while (m.find()) {
				AssignedToken res = new AssignedToken();
				res.name = tokenName;
				res.token = t;
				res.value = m.group();
				res.start = m.start();
				tokens.add(res);
			}
		}
		tokens.sort((AssignedToken o1, AssignedToken o2) -> {
			return o1.start - o2.start;
		});
		int last = 0;
		ArrayList<AssignedToken> undefined = new ArrayList<>();

		for (AssignedToken assignedToken : tokens) {
			if(assignedToken.start > last) {
				AssignedToken undef = new AssignedToken(content.substring(last, assignedToken.start), Tokenizer.UNDIFINED, "UNDEFINED");
				undef.start = last;
				undefined.add(undef);
				last += undef.value.length();
			}else if(last > assignedToken.start) {
				throw new ParsingException("Tokens overlapping: "+assignedToken+" overlaps previous Token! Last token ended at "+last+" and this token startet at "+assignedToken.start);
			}
			if(!assignedToken.name.equals(Tokens.NEW_LINE.name())) {
				last += assignedToken.value.length();
			}else {
				last += 2;
			}
		}
		
		tokens.addAll(undefined);
		tokens.sort((AssignedToken o1, AssignedToken o2) -> {
			return o1.start - o2.start;
		});
		return tokens;
	}

	/**
	 * Creates an Instance of Tokenizer.java
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 */
	public Tokenizer() {
		for (Tokens t : Tokens.values()) {
			tokens.put(t.name(), new Token(t.pattern));
		}
	}

}
