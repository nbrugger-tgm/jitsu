package com.niton.jainparse.parser;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarName;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.api.GrammarReferenceMap;
import com.niton.jainparse.token.*;
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
public abstract class Parser<R, T extends Enum<T> & Tokenable> {
    private Tokenizer<T> tokenizer = new Tokenizer<>();
    /**
     * This reference is used to resolve the root grammar
     */
    private GrammarReference<T> reference;
    private String root;

    /**
     * @param references a collection of all used Grammars
     * @param root       the grammar to be used as root
     */

    protected Parser(@NonNull GrammarReference<T> references, @NonNull Grammar<?,T> root) {
        this(references, root.getName());
    }

    /**
     * @param root the name of the grammar to use
     * @see #Parser(GrammarReference, Grammar)
     */
    protected Parser(@NonNull GrammarReference<T> csv, @NonNull String root) {
        setReference(csv);
        this.root = root;
    }

    /**
     * @param references a collection of all used Grammars
     * @param root       the grammar to be used as root
     */
    protected Parser(@NonNull GrammarReference<T> references, @NonNull GrammarName root) {
        this(references, root.getName());
    }

    protected Parser(@NonNull Grammar<?,T> rootGrammar) {
        setReference(new GrammarReferenceMap<T>().map(rootGrammar));
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
    public abstract R convert(@NonNull AstNode<T> o);

    @NotNull
    public ParsingResult<? extends AstNode<T>> parsePlain(@NonNull String content) {
        var tokens = tokenizer.tokenize(content).map(TokenStream::of);
        if(!tokens.wasParsed())
            return ParsingResult.error(tokens.exception());
        else
            return parsePlain(tokens.unwrap());
    }

    @NotNull
    public ParsingResult<? extends AstNode<T>> parsePlain(@NonNull TokenStream<T> content) {
        return reference.get(root).parse(content, reference);
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull TokenStream<T> content) {
        return parsePlain(content).map(this::convert);
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull Reader content) {
        return parsePlain(content).map(this::convert);
    }

    @NotNull
    public ParsingResult<? extends AstNode<T>> parsePlain(@NonNull Reader content) {
        return parsePlain(new TokenSource<>(content, tokenizer));
    }

    @NotNull
    public ParsingResult<? extends AstNode<T>> parsePlain(@NonNull TokenSource<T> tokens) {
        return parsePlain(TokenStream.of(tokens));
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull TokenSource<T> tokens) {
        return parsePlain(tokens).map(this::convert);
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull List<Tokenizer.AssignedToken<T>> content) {
        return parsePlain(content).map(this::convert);
    }

    @NotNull
    public ParsingResult<? extends AstNode<T>> parsePlain(@NonNull List<Tokenizer.AssignedToken<T>> content) {
        return parsePlain(TokenStream.of(content));
    }

    @NotNull
    public ParsingResult<R> parse(@NonNull InputStream content) {
        return parsePlain(content).map(this::convert);
    }

    @NotNull
    public ParsingResult<? extends AstNode<T>> parsePlain(@NonNull InputStream content) {
        return parsePlain(new TokenSource<>(content, tokenizer));
    }
}
