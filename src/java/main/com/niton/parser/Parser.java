package com.niton.parser;

import java.io.*;

import com.niton.media.filesystem.NFile;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.specific.grammar.GrammarFileContent;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer;

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
		this(references,root.getName());
	}
	/**
	 * @param references a collection of all used Grammars
	 * @param root the grammar to be used as root
	 */
	public Parser(GrammarReference references,GrammarName root) {
		this(references,root.getName());
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
	
	public GrammarResult parsePlain(String content) throws ParsingException {
		return reference.get(root).parse(new TokenStream(tokenizer.tokenize(content)), reference);
	}
	public R parse(Reader content) throws ParsingException, IOException {
		BufferedReader reader = new BufferedReader(content);
		StringBuilder bldr = new StringBuilder();
		String ln;
		while((ln = reader.readLine()) != null) {
			bldr.append(ln);
			bldr.append(System.lineSeparator());
		}
		return parse(bldr.toString());
	}
	public void add(GrammarFileContent res){
		tokenizer.tokens.putAll(res.getTokens());
		if(reference instanceof GrammarReferenceMap)
			((GrammarReferenceMap) reference).add(res);
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
	 * This converts the result of parsing {@link GrammarResult} into a Custom Type
	 * @param o the GrammarObject to convert
	 * @return an instance of the targetet Type
	 * @throws ParsingException
	 */
	public abstract R convert(GrammarResult o) throws ParsingException;
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
