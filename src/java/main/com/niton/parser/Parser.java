package com.niton.parser;

import com.niton.parser.check.Grammar;

/**
 * This is the Parser Class
 * @author Nils
 * @version 2019-05-28
 */
public class Parser {
    private Tokenizer t = new Tokenizer();
    private Grammar g;
    /**
     * Creates an Instance of Parser.java
     * @author Nils
     * @version 2019-05-29
     * @param csv
     */
    public Parser(Grammar csv) {
	setG(csv);
    }
    /**
     * @return the t
     */
    public Tokenizer getT() {
        return t;
    }
    /**
     * @param t the t to set
     */
    public void setT(Tokenizer t) {
        this.t = t;
    }
    /**
     * @return the g
     */
    public Grammar getG() {
        return g;
    }
    /**
     * @param g the g to set
     */
    public void setG(Grammar g) {
        this.g = g;
    }
    /**
     * Description : 
     * @author Nils
     * @version 2019-05-29
     * @param string
     * @throws ParsingException 
     */
    public GrammarObject parse(String string) throws ParsingException {
	return g.check(t.parse(string));
    }
}

