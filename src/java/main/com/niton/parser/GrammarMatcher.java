package com.niton.parser;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.TokenStream;

/**
 * Contains the logic how to handle a specific grammar. It creates a {@link GrammarResult} out of tokens and marks them as used for the next Executor "consuming" them
 *
 * @author Nils Brugger
 * @version 2019-06-07
 */
public abstract class GrammarMatcher<T extends GrammarResult> {
    private String originGrammarName;



    /**
     * same as {@link GrammarMatcher#parse(TokenStream, GrammarReference)} but only checks after the *pos*(parameter) token
     * @return the parsed {@link GrammarResult}
     * @throws ParsingException when the tokens are not parsable into this Grammar
     * @author Nils
     * @version 2019-05-29
     */
    public T parse(TokenStream tokens, GrammarReference reference) throws ParsingException {
//        boolean root = tokens.level() == 1;
        tokens.elevate();
        T res = process(tokens, reference);
        if(res == null)
            throw new ParsingException(this.getClass().getSimpleName()+".process(..) returned null. This is forbidden please throw an Exception instead");
        res.setOriginGrammarName(getOriginGrammarName());
//        if(root && tokens.index()+1<tokens.size())
//            throw new ParsingException("Not all tokens consumed at the end of parsing");
        return res;
    }



    /**
     * The parsing process itself
     * use {@link TokenStream#next()} ()} to iterate over the tokens
     * if the tokens are not parsable into the expected Grammar throw an exception
     * @param tokens the tokens representing the tokenized string to parse
     * @param reference the collection to get Grammars from
     * @return the grammar object @NotNull
     * @throws ParsingException if anything goes wrong in the parsing process
     */
    protected abstract T process(TokenStream tokens, GrammarReference reference) throws ParsingException;

    public String getOriginGrammarName() {
        return originGrammarName;
    }

    public GrammarMatcher setOriginGrammarName(String originGrammarName) {
        this.originGrammarName = originGrammarName;
        return this;
    }

}

