package com.niton.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A token describes a group of chars which comonly appear next to each other or single characters.
 * Works with RegEx. Features only to match when a special regex is bevore or after the main expression
 *
 * @author Nils
 * @version 2019-05-27
 */
public class Token {
    private static final String tokenReplacer = "{main}";
    private static final String leadingReplacer = "{lead}";
    private static final String trailingReplacer = "{trail}";
    private static final String template = "(?<=" + leadingReplacer + ")(" + tokenReplacer + ")(?=" + trailingReplacer + ")";
    private List<Token> leading = new LinkedList<>();
    private List<Token> trailing = new LinkedList<>();
    private Pattern regex;

    /**
     * Creates an Instance of Token.java
     *
     * @author Nils
     * @version 2019-05-27
     */
    public Token(String regex) {
        this.regex = Pattern.compile(regex, Pattern.MULTILINE);
    }

    /**
     * Creates an Instance of Token.java
     *
     * @param regex
     * @author Nils
     * @version 2019-05-27
     */
    public Token(Pattern regex) {
        super();
        this.regex = regex;
    }

    /**
     * @return the leading
     */
    public List<Token> getLeading() {
        return leading;
    }

    /**
     * @return the trailing
     */
    public List<Token> getTrailing() {
        return trailing;
    }

    /**
     * @param regex the regex to set
     */
    public void setRegex(Pattern regex) {
        this.regex = regex;
    }

    /**
     * @param regex the regex to set
     */
    public void setRegex(String regex) {
        this.regex = Pattern.compile(regex, Pattern.MULTILINE);
    }

    /**
     * Creates an Instance of Token.java
     *
     * @param leading
     * @param trailing
     * @param regex
     * @author Nils
     * @version 2019-05-27
     */
    public Token(ArrayList<Token> leading, ArrayList<Token> trailing, Pattern regex) {
        super();
        this.leading = leading;
        this.trailing = trailing;
        this.regex = regex;
    }


    public Token(Token leading, Token trailing, Pattern regex) {
        super();
        this.leading = new ArrayList<>(1);
        this.trailing = new ArrayList<>(1);
        this.leading.add(leading);
        this.trailing.add(trailing);
        this.regex = regex;
    }

    public Token(Token leading, Token trailing, String regex) {
        super();
        this.leading = new ArrayList<>(1);
        this.trailing = new ArrayList<>(1);
        this.leading.add(leading);
        this.trailing.add(trailing);
        this.regex = Pattern.compile(regex, Pattern.MULTILINE);
    }

    /**
     * @return the compiled main regex (NOT contains tail/leading)
     */
    public Pattern getRegex() {
        return regex;
    }


    /**
     * @param leading the leading to set
     */
    public void setLeading(Token leading) {
        this.leading.clear();
        this.leading.add(leading);
    }

    /**
     * @param trailing the trailing to set
     */
    public void setTrailing(Token trailing) {
        this.trailing.clear();
        this.trailing.add(trailing);
    }

    /**
     * @param arg0
     * @return
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public boolean addLeading(Token arg0) {
        return leading.add(arg0);
    }

    /**
     * @see java.util.ArrayList#clear()
     */
    public void clearLeading() {
        leading.clear();
    }

    /**
     * @param arg0
     * @return
     * @see java.util.ArrayList#remove(java.lang.Object)
     */
    public boolean removeLeading(Object arg0) {
        return leading.remove(arg0);
    }

    /**
     * @param arg0
     * @return
     * @see java.util.List#add(java.lang.Object)
     */
    public boolean addTrailing(Token arg0) {
        return trailing.add(arg0);
    }


    /**
     * @param arg0
     * @return
     * @see java.util.ArrayList#remove(java.lang.Object)
     */
    public boolean removeTrailing(Object arg0) {
        return trailing.remove(arg0);
    }

    /**
     * @return the compiled complete regex (contains tail/leading)
     */
    public Pattern getCompletePattern() {
        return Pattern.compile(template.replace(tokenReplacer, regex.pattern()).replace(trailingReplacer, getTrailingRegex()).replace(leadingReplacer, getLeadingRegex()), Pattern.MULTILINE);
    }

    /**
     * Description :
     *
     * @return
     * @author Nils
     * @version 2019-05-27
     */
    private CharSequence getLeadingRegex() {
        return buildCondition(leading);
    }

    /**
     * Description :
     *
     * @return
     * @author Nils
     * @version 2019-05-27
     */
    private CharSequence getTrailingRegex() {
        return buildCondition(trailing);
    }

    private CharSequence buildCondition(List<Token> trailing) {
        if (trailing.size() == 0)
            return ".*";
        if (trailing.size() == 1)
            return trailing.get(0).getCompletePattern().pattern();
        else {
            StringBuilder builder = new StringBuilder();
            for (Token token : trailing) {
                builder.append('(');
                builder.append(token.getCompletePattern().pattern());
                builder.append(')');
                builder.append('|');
            }
            builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        }
    }
}

