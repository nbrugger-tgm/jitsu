package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Contains the logic how to handle a specific grammar. It creates a {@link GrammarObject} out of tokens and marks them as used for the next Executor "consuming" them
 *
 * @author Nils Brugger
 * @version 2019-06-07
 */
public abstract class GrammarExecutor {
    private int index;
    private String name;

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Generates an Grammar object from the list of tokens provided
     * @throws ParsingException  when the tokens are not parsable into this Grammar
     * @author Nils
     * @version 2019-05-28
     */
    public final GrammarObject check(ArrayList<AssignedToken> tokens, GrammarReference reference) throws ParsingException {
        index = 0;
        return process(tokens, reference);
    }

    /**
     * same as {@link GrammarExecutor#check(ArrayList, GrammarReference)} but only checks after the *pos*(parameter) token
     * @param pos the number of allready consumed tokens
     * @return the parsed {@link GrammarObject}
     * @throws ParsingException when the tokens are not parsable into this Grammar
     * @author Nils
     * @version 2019-05-29
     */
    public GrammarObject check(ArrayList<AssignedToken> tokens, int pos, GrammarReference reference) throws ParsingException {
        index = pos;
        if (index >= tokens.size())
            throw new ParsingException("No More Tokens!");
        return process(tokens, reference);
    }

    /**
     * Marks the next token as consumed
     */
    protected final void increase() {
        index++;
    }

    /**
     * @return the index of the token you are at
     */
    public int index() {
        return index;
    }

    /**
     * @return the index
     */
    public void index(int index) {
        this.index = index;
    }

    /**
     * The parsing process itself
     * use {@link GrammarExecutor#index()} and {@link GrammarExecutor#increase()} to iterate over the tokens
     * if the tokens are not parsable into the expected Grammar throw an exception
     * @param tokens the tokens representing the tokenized string to parse
     * @param reference the collection to get Grammars from
     * @return the grammar object @NotNull
     * @throws ParsingException if anything goes wrong in the parsing process
     */
    protected abstract GrammarObject process(ArrayList<AssignedToken> tokens, GrammarReference reference) throws ParsingException;
}

