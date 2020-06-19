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

/**
 * The parser superset.
 * This classes use is to make the complete Parsing process
 * @author Nils
 * @version 2019-05-28
 * @param <R> the class to parse into
 */
public abstract class Parser<R> {
	private Tokenizer tokenizer = new Tokenizer();
	private GrammarReference reference;
	private String root;

	/**
	 * @param references a collection of all used Grammars
	 * @param root the grammar to be used as root
	 */
	public Parser(GrammarReference references,Grammar root) {
		setReference(references);
		this.root = root.getName();
	}

	/**
	 * @see #Parser(GrammarReference, Grammar)
	 * @param root the name of the grammar to use
	 */
	public Parser(GrammarReference csv,String root) {
		setReference(csv);
		this.root = root;
	}

	public GrammarReference getReference() {
		return reference;
	}


	public String getRoot() {
		return root;
	}

	public Tokenizer getTokenizer() {
		return tokenizer;
	}

	/**
	 * Parses the text into a instance of R
	 * @author Nils
	 * @version 2019-05-29
	 * @param content the string to parse
	 * @throws ParsingException
	 */
	public R parse(String content) throws ParsingException {
		return convert(parsePlain(content));
	}
	
	public GrammarObject parsePlain(String content) throws ParsingException {
		return reference.get(root).getExecutor().check(tokenizer.parse(content), reference);
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
	 * @param reference the g to set
	 */
	public void setReference(GrammarReference reference) {
		this.reference = reference;
	}
	/**
	 * @param root the root to set
	 */
	public void setRoot(String root) {
		this.root = root;
	}
	/**
	 * @param tokenizer the t to set
	 */
	public void setTokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	/**
	 * This converts the result of parsing {@link GrammarObject} into a Custom Type
	 * @param o the GrammarObject to convert
	 * @return an instance of the targetet Type
	 * @throws ParsingException
	 */
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
