package com.niton.jainparse.grammar.api;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenable;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Stack;

/**
 * Contains the logic how to handle a specific grammar. It creates a {@link AstNode} out of
 * tokens and marks them as used for the next Executor "consuming" them
 *
 * @author Nils Brugger
 * @version 2019-06-07
 */
public abstract class GrammarMatcher<T extends AstNode<N>, N extends Enum<N> & Tokenable> {
    @Setter
    @Getter
    private String originGrammarName;
    private String originIdentifier;
    static public String additionalInfo = "";

    static final ThreadLocal<Stack<String>> recursionStack = ThreadLocal.withInitial(Stack::new);

    /**
     * same as {@link GrammarMatcher#parse(TokenStream, GrammarReference)} but only checks after the
     * *pos*(parameter) token
     *
     * @return the parsed {@link AstNode}
     * @ when the tokens are not parsable into this Grammar
     * @author Nils
     * @version 2019-05-29
     */
    @NotNull
    public ParsingResult<T> parse(@NonNull TokenStream<N> tokens, @NonNull GrammarReference<N> reference) {
        boolean root = tokens.level() == 0;
        try {
            tokens.elevate();
            if(originGrammarName != null) {
                recursionStack.get().push(originGrammarName + "(" + additionalInfo + "-"+tokens.index() + ")");
                additionalInfo = "";
            }
        } catch (IllegalStateException e) {
            return ParsingResult.error(new ParsingException(
                    getIdentifier(),
                    String.format("Parsing %s failed: %s", originGrammarName, e.getMessage()),
                    tokens.currentLocation()
            ));
        }

        var result = process(tokens, reference);
        if (!result.wasParsed()) {
            tokens.rollback();
            if(originGrammarName != null)
                recursionStack.get().pop();
            return result;
        }
        tokens.commit();
        if(originGrammarName != null)
            recursionStack.get().pop();
        var node = result.unwrap();
        node.setOriginGrammarName(getOriginGrammarName());
        if (root && tokens.hasNext()) {
            var token = tokens.next();
            if (node.getParsingException() == null) {
                return ParsingResult.error(new ParsingException(
                        getIdentifier(),
                        "Not all tokens consumed at the end of parsing (next token: " + token + ")",
                        tokens.currentLocation()
                ));
            } else {
                return ParsingResult.error(node.getParsingException());
            }
        }
        return result;
    }

    /**
     * The parsing process itself use {@link TokenStream#next()} to iterate over the tokens.
     * Behaviour contract:
     * <ul>
     *     <li>When parsing is successfull return the result</li>
     *     <li>When parsing is not successfull throw a {@link ParsingException}</li>
     * </ul>
     * <b>Do not use {@link TokenStream#commit()} or {@link TokenStream#rollback()} unless you opened a new frame yourself</b>
     *
     * @param tokens    the tokens representing the tokenized string to parse
     * @param reference the collection to get Grammars from
     * @return the result of the parsing process. Error or AstNode @NotNull
     */
    @NotNull
    protected abstract ParsingResult<T> process(@NotNull TokenStream<N> tokens, @NotNull GrammarReference<N> reference);

    public String getIdentifier() {
        return originIdentifier;
    }

    public void setIdentifier(String identifier) {
        this.originIdentifier = identifier;
    }
}

