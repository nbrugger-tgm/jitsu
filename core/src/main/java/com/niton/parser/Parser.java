package com.niton.parser;

import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarReferenceMap;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarName;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.ListTokenStream;
import com.niton.parser.token.TokenSource;
import com.niton.parser.token.TokenStream;
import com.niton.parser.token.Tokenizer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

/**
 * The parser superset.
 * This classes use is to make the complete Parsing process
 *
 * @param <R> the class to parse into
 *
 * @author Nils
 * @version 2019-05-28
 */
@Getter
@Setter
public abstract class Parser<R> {
	private Tokenizer        tokenizer = new Tokenizer();
	/**
	 * This reference is used to resolve the root grammar
	 */
	private GrammarReference reference;
	private String           root;

	/**
	 * @param references a collection of all used Grammars
	 * @param root       the grammar to be used as root
	 */

	protected Parser(@NonNull GrammarReference references, @NonNull Grammar<?> root) {
		this(references, root.getName());
	}

	/**
	 * @param root the name of the grammar to use
	 *
	 * @see #Parser(GrammarReference, Grammar)
	 */
	protected Parser(@NonNull GrammarReference csv, @NonNull String root) {
		setReference(csv);
		this.root = root;
	}

	/**
	 * @param references a collection of all used Grammars
	 * @param root       the grammar to be used as root
	 */
	protected Parser(@NonNull GrammarReference references, @NonNull GrammarName root) {
		this(references, root.getName());
	}

	protected Parser(@NonNull Grammar<?> rootGrammar) {
		setReference(new GrammarReferenceMap().map(rootGrammar));
		this.root = rootGrammar.getName();
	}

	@NotNull
	public R parse(@NonNull String content) throws ParsingException {
		return convert(parsePlain(content));
	}

	/**
	 * This converts the result of parsing {@link AstNode} into a Custom Type
	 *
	 * @param o the GrammarObject to convert
	 *
	 * @return an instance of the targeted Type
	 */
	@NotNull
	public abstract R convert(@NonNull AstNode o) throws ParsingException;

	@NotNull
	public AstNode parsePlain(@NonNull String content) throws ParsingException {
		return parsePlain(new ListTokenStream(tokenizer.tokenize(content)));
	}

	@NotNull
	public AstNode parsePlain(@NonNull TokenStream content) throws ParsingException {
		return reference.get(root).parse(content, reference);
	}

	@NotNull
	public R parse(@NonNull TokenStream content) throws ParsingException {
		return convert(parsePlain(content));
	}

	@NotNull
	public R parse(@NonNull Reader content) throws ParsingException {
		return convert(parsePlain(content));
	}

	@NotNull
	public AstNode parsePlain(@NonNull Reader content) throws ParsingException {
		return parsePlain(new TokenSource(content));
	}

	@NotNull
	public AstNode parsePlain(@NonNull TokenSource tokens) throws ParsingException {
		return parsePlain(new ListTokenStream(tokens));
	}

	@NotNull
	public R parse(@NonNull TokenSource tokens) throws ParsingException {
		return convert(parsePlain(tokens));
	}

	@NotNull
	public R parse(@NonNull List<Tokenizer.AssignedToken> content) throws ParsingException {
		return convert(parsePlain(content));
	}

	@NotNull
	public AstNode parsePlain(@NonNull List<Tokenizer.AssignedToken> content)
			throws ParsingException {
		return parsePlain(new ListTokenStream(content));
	}

	@NotNull
	public R parse(@NonNull InputStream content) throws ParsingException {
		return convert(parsePlain(content));
	}

	@NotNull
	public AstNode parsePlain(@NonNull InputStream content) throws ParsingException {
		return parsePlain(new TokenSource(content));
	}
}
