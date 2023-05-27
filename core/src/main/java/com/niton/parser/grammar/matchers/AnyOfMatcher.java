package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AnyNode;
import com.niton.parser.ast.AstNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.MultiGrammar;
import com.niton.parser.token.TokenStream;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * Checks against all given Grammars syncron and returns the first matching
 *
 * @author Nils
 * @version 2019-05-29
 */
public class AnyOfMatcher extends GrammarMatcher<AnyNode> {
    private MultiGrammar grammars;

    public AnyOfMatcher(MultiGrammar grammers) {
        this.grammars = grammers;
    }

    /**
     * @param tokens
     * @param ref
     * @see GrammarMatcher#process(TokenStream, GrammarReference)
     */
    @Override
    public @NotNull AnyNode process(@NotNull TokenStream tokens, @NotNull GrammarReference ref)
            throws ParsingException {
        Map<String, ParsingException> fails = new HashMap<>();
        for (var grammar : this.grammars.getGrammars()) {
            try {
                AstNode obj = grammar.parse(tokens, ref);
                AnyNode wrapper = new AnyNode(obj);
                if (obj.getParsingException() != null)
                    fails.put(grammar.getIdentifier(), obj.getParsingException());
                if(fails.size() > 0){
                    wrapper.setParsingException(new ParsingException(getIdentifier(), String.format(
                            "Expected one of : [%s] and some were not parsable",
                            Arrays.stream(this.grammars.getGrammars()).map(Grammar::getIdentifier).collect(joining(", "))
                    ), formatFails(fails)));
                }
                return wrapper;
            } catch (ParsingException e) {
                fails.put(grammar.getIdentifier(), e);
            }
        }
        throw new ParsingException(getIdentifier(), String.format(
                "Expected one of : [%s] but none of them was parsable",
                Arrays.stream(this.grammars.getGrammars()).map(Grammar::getIdentifier).collect(joining(", "))
        ), formatFails(fails));
    }

    private ParsingException[] formatFails(Map<String, ParsingException> fails) {
        return fails.entrySet().stream().map(e -> {
            return new ParsingException(getIdentifier(), String.format(
                    "%s: %s",
                    e.getKey(),
                    e.getValue().getMessage()
            ), e.getValue());
        }).toArray(ParsingException[]::new);
    }


    /**
     * @return the tokens
     */
    public Grammar<?, ?>[] getGrammars() {
        return grammars.getGrammars();
    }

    public void setGrammars(MultiGrammar grammars) {
        this.grammars = grammars;
    }
}
