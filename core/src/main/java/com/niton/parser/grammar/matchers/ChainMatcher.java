package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.SequenceNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.ChainGrammar;
import com.niton.parser.token.TokenStream;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Used to build a grammar<br>
 * Tests all Grammars in the chain after each other
 *
 * @author Nils
 * @version 2019-05-28
 */
@Getter
@Setter
public class ChainMatcher extends GrammarMatcher<SequenceNode> {

    private ChainGrammar chain;

    /**
     * Creates an Instance of ChainExecutor.java
     *
     * @param chain
     * @author Nils Brugger
     * @version 2019-06-08
     */
    public ChainMatcher(ChainGrammar chain) {
        this.chain = chain;
        setOriginGrammarName(chain.getName());
        setIdentifier(chain.getIdentifier());
    }

    @Override
    public @NotNull SequenceNode process(
            @NotNull TokenStream tokens,
            @NotNull GrammarReference reference
    ) throws ParsingException {
        SequenceNode gObject = new SequenceNode();
        List<ParsingException> exitStates = new ArrayList<>(chain.getChain().size());
        int i = 0;
        for (var grammar : chain.getChain()) {
            try {
                var res = grammar.parse(tokens, reference);
                String name;
                if ((name = chain.getNaming().get(i)) != null) {
                    gObject.name(name, res);
                } else {
                    gObject.add(res);
                }
                if (res.getParsingException() != null)
                    exitStates.add(softChainElementException(name != null ? name : grammar.getIdentifier(), res.getParsingException()));
//                else
//                    exitStates.add(new ParsingException(getIdentifier(), format("Chain element '%s' parsed successfully", name != null ? name : grammar.getIdentifier()), tokens));
            } catch (ParsingException e) {
                var elementName =  chain.getNaming().get(i);
                var crashingException = new ParsingException(
                        getIdentifier(),
                        format("Chain entry '%s' can't be parsed!",elementName != null ? elementName : grammar),
                        e
                );
                exitStates.add(crashingException);
                throw new ParsingException(getIdentifier(), "Chain could not be parsed!", exitStates.toArray(ParsingException[]::new));
            }finally {
                i++;
            }
            if (exitStates.size() > 0)
                gObject.setParsingException(new ParsingException(getIdentifier(), format(
                        "Chain parsed successful with allowed exceptions"
                ), exitStates.toArray(ParsingException[]::new)));
        }
        if(gObject.subNodes.size() == 0) {
            gObject.setExplicitLocation(AstNode.Location.oneChar(tokens.getLine(), tokens.getColumn()));
        }
        return gObject;
    }

    private ParsingException softChainElementException(String elementName, ParsingException parsingException) {
        return new ParsingException(getIdentifier(), format(
                "Chain element '%s' parsed successfully with allowed exception",
                elementName
        ), parsingException);
    }


}
