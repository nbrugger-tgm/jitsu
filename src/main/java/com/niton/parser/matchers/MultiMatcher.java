package com.niton.parser.matchers;

import com.niton.parser.GrammarMatcher;
import com.niton.parser.token.TokenStream;
import com.niton.parser.GrammarReference;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.Grammar;
import com.niton.parser.grammars.MultiGrammar;
import com.niton.parser.result.AnyGrammarResult;
import com.niton.parser.GrammarResult;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Checks against all given Grammars syncron and returns the first matching
 *
 * @author Nils
 * @version 2019-05-29
 */
public class MultiMatcher extends GrammarMatcher<AnyGrammarResult> {

    public void setGrammars(MultiGrammar grammars) {
        this.grammars = grammars;
    }

    private MultiGrammar grammars;

    public MultiMatcher(MultiGrammar grammers) {
        this.grammars = grammers;
    }

    /**
     * @see GrammarMatcher#process(TokenStream, GrammarReference)
     */
    @Override
    public AnyGrammarResult process(TokenStream tokens, GrammarReference ref) throws ParsingException {
        for (Grammar grammar : this.grammars.getGrammars()) {
            try {
                GrammarResult obj = grammar.parse(tokens, ref);
                if (obj == null)
                    throw new ParsingException("Tokens do not match subgrammar " + grammar.getName());
                AnyGrammarResult wrapper = new AnyGrammarResult(obj);
                wrapper.setType(grammar.getName());
                tokens.commit();
                return wrapper;
            } catch (ParsingException e) {
                tokens.rollback();
            }
        }
        throw new ParsingException(
                "Expected Grammar (OR) : [" + Arrays.stream(this.grammars.getGrammars()).map(e -> {
                    String n;
                    if ((n = e.getName()) != null) return n;
                    return e.getClass().getSimpleName();
                }).collect(Collectors.joining(", ")) + "] but none of them was parsable");
    }

    /**
     * @return the tokens
     */
    public Grammar[] getGrammars() {
        return grammars.getGrammars();
    }
}
