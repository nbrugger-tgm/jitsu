package com.niton.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import com.niton.media.filesystem.NFile;
import com.niton.parser.grammar.Grammar;
import com.niton.parser.specific.grammar.GrammarResult;

/**
 * This is the Parser Class
 * 
 * @author Nils
 * @version 2019-05-28
 */
public abstract class Parser<R> {
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
	public R parse(String content) throws ParsingException {
		return convert(parsePlain(content));
	}
	
	public GrammarObject parsePlain(String content) throws ParsingException {
		return g.get(root).getExecutor().check(t.parse(content),g);
	}
	public R parse(Reader content) throws ParsingException, IOException {
		StringBuilder buffer = new StringBuilder();
		int i = content.read();
		while(i != -1) {
			buffer.appendCodePoint(i);
		}
		return parse(buffer.toString());
	}
	
	public R parse(NFile content) throws ParsingException, IOException {
		return parse(content.getText());
	}
	public R parse(InputStream content) throws ParsingException, IOException {
		return parse(new InputStreamReader(content));
	}
	public R parse(File content) throws ParsingException, IOException {
		return parse(new FileInputStream(content));
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
	
	public abstract R convert(GrammarObject o) throws ParsingException;
	/**
	 * <b>Description :</b><br>
	 * 
	 * @author Nils Brugger
	 * @version 2019-06-13
	 * @param file
	 * @return
	 * @throws ParsingException 
	 */
	public R parse(byte[] file) throws ParsingException {
		try {
			return parse(new String(file,"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return parse(new String(file));
		}
	}
}
