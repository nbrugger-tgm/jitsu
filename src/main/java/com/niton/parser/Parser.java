package com.niton.parser;

import com.niton.media.filesystem.NFile;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.specific.grammar.GrammarFileContent;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * The parser superset.
 * This classes use is to make the complete Parsing process
 *
 * @param <R> the class to parse into
 * @author Nils
 * @version 2019-05-28
 */
public abstract class Parser<R> {
	private Tokenizer        tokenizer = new Tokenizer();
	private GrammarReference reference;
	private String           root;

	/**
	 * @param references a collection of all used Grammars
	 * @param root       the grammar to be used as root
	 */
	public Parser(GrammarReference references, Grammar root) {
		this(references, root.getName());
	}

	/**
	 * @param references a collection of all used Grammars
	 * @param root       the grammar to be used as root
	 */
	public Parser(GrammarReference references, GrammarName root) {
		this(references, root.getName());
	}

	/**
	 * @param root the name of the grammar to use
	 * @see #Parser(GrammarReference, Grammar)
	 */
	public Parser(GrammarReference csv, String root) {
		setReference(csv);
		this.root = root;
	}

	public Parser(Grammar<?, ?> rootGrammar) {
		setReference(new GrammarReferenceMap().map(rootGrammar));
		this.root = rootGrammar.getName();
	}

	public GrammarReference getReference() {
		return reference;
	}

	/**
	 * @param reference the g to set
	 */
	public void setReference(GrammarReference reference) {
		this.reference = reference;
	}

	public String getRoot() {
		return root;
	}

	/**
	 * @param root the root to set
	 */
	public void setRoot(String root) {
		this.root = root;
	}

	public Tokenizer getTokenizer() {
		return tokenizer;
	}

	/**
	 * @param tokenizer the t to set
	 */
	public void setTokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	/**
	 * Parses the text into a instance of R
	 *
	 * @param content the string to parse
	 * @throws ParsingException
	 * @author Nils
	 * @version 2019-05-29
	 */
	public R parse(String content) throws ParsingException {
		return convert(parsePlain(content));
	}

	public GrammarResult parsePlain(String content) throws ParsingException {
		return reference.get(root).parse(new TokenStream(tokenizer.tokenize(content)), reference);
	}

	public R parse(Reader content) throws ParsingException, IOException {
		BufferedReader reader = new BufferedReader(content);
		StringBuilder  bldr   = new StringBuilder();
		String         ln;
		while ((ln = reader.readLine()) != null) {
			bldr.append(ln);
			bldr.append(System.lineSeparator());
		}
		return parse(bldr.toString());
	}

	public void add(GrammarFileContent res) {
		tokenizer.tokens.putAll(res.getTokens());
		if (reference instanceof GrammarReferenceMap) {
			((GrammarReferenceMap) reference).add(res);
		}
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
	 * This converts the result of parsing {@link GrammarResult} into a Custom Type
	 *
	 * @param o the GrammarObject to convert
	 * @return an instance of the targetet Type
	 * @throws ParsingException
	 */
	public abstract R convert(GrammarResult o) throws ParsingException;

	/**
	 * <b>Description :</b><br>
	 *
	 * @param file
	 * @return
	 * @throws ParsingException
	 * @author Nils Brugger
	 * @version 2019-06-13
	 */
	public R parse(byte[] file) throws ParsingException {
		return parse(new String(file, StandardCharsets.UTF_8));
	}
}
