package com.niton.parser;

import com.niton.parser.grammar.Grammar;

/**
 * This is the Parser Class
 * 
 * @author Nils
 * @version 2019-05-28
 */
public class Parser {
	private Tokenizer t = new Tokenizer();
	private GrammarReference g;
	private String root;

	public Parser(GrammarReference csv,Grammar root) {
		setG(csv);
		this.root = root.getName();
	}
	/**
	 * Creates an Instance of Parser.java
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param csv
	 */
	public Parser(GrammarReference csv,String root) {
		setG(csv);
		this.root = root;
	}
	
	/**
	 * @return the g
	 */
	public GrammarReference getG() {
		return g;
	}

	/**
	 * @return the root
	 */
	public String getRoot() {
		return root;
	}

	/**
	 * @return the t
	 */
	public Tokenizer getT() {
		return t;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param string
	 * @throws ParsingException
	 */
	public GrammarObject parse(String content) throws ParsingException {
		return g.get(root).getExecutor().check(t.parse(content),g);
	}
	/**
	 * @param g the g to set
	 */
	public void setG(GrammarReference g) {
		this.g = g;
	}
	/**
	 * @param root the root to set
	 */
	public void setRoot(String root) {
		this.root = root;
	}
	/**
	 * @param t the t to set
	 */
	public void setT(Tokenizer t) {
		this.t = t;
	}
}
