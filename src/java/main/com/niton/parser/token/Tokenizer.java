package com.niton.parser.token;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.Tokenable;
import com.niton.parser.specific.grammar.GrammarFileContent;

import java.util.*;
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
    public Map<String, Token> tokens = new HashMap<>();
    private boolean ignoreEOF = false;

    /**
     * Creates a tokenizer based on {@link DefaultToken}
     */
    public Tokenizer() {
        this(DefaultToken.values());
    }

    void add(GrammarFileContent result) {
        tokens.putAll(result.tokens);
    }

    /**
     * {@link Tokenizer#add(Tokenable[])}
     *
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
    public List<AssignedToken> tokenize(String content) throws ParsingException {
        List<AssignedToken> tokens = new LinkedList<>();
        for (String tokenName : this.tokens.keySet()) {
            Token t = this.tokens.get(tokenName);
            if(ignoreEOF && t.getRegex().pattern().equals(DefaultToken.EOF.pattern))
                continue;
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
        List<AssignedToken> undefined = new ArrayList<>(tokens.size());

        for (AssignedToken assignedToken : tokens) {
            if (assignedToken.start > last) {
                AssignedToken undef = new AssignedToken(content.substring(last, assignedToken.start), Tokenizer.UNDIFINED, "UNDEFINED");
                undef.start = last;
                undefined.add(undef);
                last += undef.value.length();
            } else if (last > assignedToken.start) {
                throw new ParsingException("Tokens overlapping: " + assignedToken + " overlaps previous Token! Last token ended at " + last + " and this token startet at " + assignedToken.start + " (" + content.substring(last - 5, last + 5) + ")");
            }
            last += assignedToken.value.length();
        }

        tokens.addAll(undefined);
        tokens.sort(Comparator.comparingInt((AssignedToken o) -> o.start));
        return tokens;
    }

    /**
     * Adds tokens the Tokenizer understands
     *
     * @param tokens the tokens to add
     */
    public void add(Tokenable[] tokens) {
        for (Tokenable t : tokens) {
            this.tokens.put(t.name(), new Token(t.pattern()));
        }
    }

    public boolean isIgnoreEOF() {
        return ignoreEOF;
    }

    public void setIgnoreEOF(boolean ignoreEOF) {
        this.ignoreEOF = ignoreEOF;
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
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return value + " (" + name + ")";
        }
    }
}
