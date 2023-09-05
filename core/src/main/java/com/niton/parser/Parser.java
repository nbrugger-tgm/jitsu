package com.niton.parser;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.ParsingResult;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarName;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.api.GrammarReferenceMap;
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
 * @author Nils
 * @version 2019-05-28
 */
@Getter
@Setter
public abstract class Parser<R> {
    private Tokenizer tokenizer = new Tokenizer();
    /**
     * This reference is used to resolve the root grammar
     */
    private GrammarReference reference;
    private String root;

    /**
     * @param references a collection of all used Grammars
     * @param root       the grammar to be used as root
     */

    protected Parser(@NonNull GrammarReference references, @NonNull Grammar<?> root) {
        this(references, root.getName());
    }

    /**
     * @param root the name of the grammar to use
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
    public ParsingResult<R> parse(@NonNull String content) {
        return parsePlain(content).map(this::convert);
    }

    /**
     * This converts the result of parsing {@link AstNode} into a Custom Type
     *
     * @param o the GrammarObject to convert
     * @return an instance of the targeted Type
     */
    @NotNull
    public abstract R convert(@NonNull AstNode o);

    @NotNull
    public ParsingResult<? extends AstNode> parsePlain(@NonNull String content) {
        var tokens = tokenizer.tokenize(content).map(ListTokenStream::new);
        if(!tokens.wasParsed())
            return ParsingResult.error(tokens.exception());
        else
            return parsePlain(tokens.unwrap());
    }

    @NotNull
    public ParsingResult<? extends AstNode> parsePlain(@NonNull TokenStream content) {
        return reference.get(root).parse(content, reference);
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull TokenStream content) {
        return parsePlain(content).map(this::convert);
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull Reader content) {
        return parsePlain(content).map(this::convert);
    }

    @NotNull
    public ParsingResult<? extends AstNode> parsePlain(@NonNull Reader content) {
        return parsePlain(new TokenSource(content));
    }

    @NotNull
    public ParsingResult<? extends AstNode> parsePlain(@NonNull TokenSource tokens) {
        return parsePlain(new ListTokenStream(tokens));
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull TokenSource tokens) {
        return parsePlain(tokens).map(this::convert);
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull List<Tokenizer.AssignedToken> content) {
        return parsePlain(content).map(this::convert);
    }

    @NotNull
    public ParsingResult<? extends AstNode> parsePlain(@NonNull List<Tokenizer.AssignedToken> content) {
        return parsePlain(new ListTokenStream(content));
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull InputStream content) {
        return parsePlain(content).map(this::convert);
    }

    @NotNull
    public ParsingResult<? extends AstNode> parsePlain(@NonNull InputStream content) {
        return parsePlain(new TokenSource(content));
    }
}
