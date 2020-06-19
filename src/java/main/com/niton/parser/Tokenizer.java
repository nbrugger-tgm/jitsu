package com.niton.parser;

import com.niton.parser.grammar.Tokenable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The tokenizer is the first step of parsing and devides the string into classified chunks ({@link AssignedToken}s
 *
 * @author Nils
 * @version 2019-05-27
 */
public class Tokenizer {
    /**
     * This token is used if no other token fits the string
     */
    public static final Token UNDIFINED = new Token(".+");
    public HashMap<String, Token> tokens = new HashMap<>();

    /**
     * Creates an Instance of Tokenizer.java
     *
     * @author Nils
     * @version 2019-05-28
     */
    public Tokenizer() {
        this(Tokens.values());
    }

    /**
     * {@link Tokenizer#add(Tokenable[])}
     * @param tokens
     */
    public Tokenizer(Tokenable[] tokens) {
        for (Tokenable t : tokens) {
            this.tokens.put(t.name(), new Token(t.pattern()));
        }
    }

    /**
     * Tokenizes the string. Maps each part of the string to a token. If no token in the tokenizer matches a certain part {@link Tokenizer#UNDIFINED} is used
     *
     * @param content the string to map to tokens
     * @return a list of assigned tokens.
     * @throws ParsingException
     */
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
        tokens.sort(Comparator.comparingInt((AssignedToken o) -> o.start));
        int last = 0;
        ArrayList<AssignedToken> undefined = new ArrayList<>();

        for (AssignedToken assignedToken : tokens) {
            if (assignedToken.start > last) {
                AssignedToken undef = new AssignedToken(content.substring(last, assignedToken.start), Tokenizer.UNDIFINED, "UNDEFINED");
                undef.start = last;
                undefined.add(undef);
                last += undef.value.length();
            } else if (last > assignedToken.start) {
                throw new ParsingException("Tokens overlapping: " + assignedToken + " overlaps previous Token! Last token ended at " + last + " and this token startet at " + assignedToken.start + " (" + content.substring(last - 5, last + 5) + ")");
            }
            //TODO: apply a more gernal not hardcoded way
            if (!assignedToken.name.equals(Tokens.NEW_LINE.name())) {
                last += assignedToken.value.length();
            } else {
                last += 2;
            }
            //TODO: ---||---
        }

        tokens.addAll(undefined);
        tokens.sort((AssignedToken o1, AssignedToken o2) -> {
            return o1.start - o2.start;
        });
        return tokens;
    }

    /**
     * Adds tokens the Tokenizer understands
     * @param tokens the tokens to add
     */
    public void add(Tokenable[] tokens) {
        for (Tokenable t : tokens) {
            this.tokens.put(t.name(), new Token(t.pattern()));
        }
    }

    /**
     * An assigned token defines which token matches the part of the string stored in {@link AssignedToken#value}
     */
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

        public AssignedToken() {
        }

        /**
         * returns the matched string and the name of the token
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return value+ " (" + name + ")";
        }
    }
}
