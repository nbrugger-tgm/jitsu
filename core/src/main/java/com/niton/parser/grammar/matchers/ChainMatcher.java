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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<ParsingException> exitStates = new ArrayList<>(chain.getChain().size());
        Map<String, Integer> naming = new HashMap<>();
        List<AstNode> subNodes = new ArrayList<>();
        int i = 0;
        for (var grammar : chain.getChain()) {
            try {
                var res = grammar.parse(tokens, reference);

                subNodes.add(res);
                String name;
                if ((name = chain.getNaming().get(i)) != null) {
                    naming.put(name, i);
                }
                if (res.getParsingException() != null)
                    exitStates.add(softChainElementException(name != null ? name : grammar.getIdentifier(), res.getParsingException()));
//                else
//                    exitStates.add(new ParsingException(getIdentifier(), format("Chain element '%s' parsed successfully", name != null ? name : grammar.getIdentifier()), tokens));
            } catch (ParsingException e) {
                var elementName = chain.getNaming().get(i);
                var crashingException = new ParsingException(
                        getIdentifier(),
                        format("Chain entry '%s' can't be parsed!", elementName != null ? elementName : grammar),
                        e
                );
                exitStates.add(crashingException);
                throw new ParsingException(getIdentifier(), "Chain could not be parsed!", exitStates.toArray(ParsingException[]::new));
            } finally {
                i++;
            }
        }
        if (subNodes.isEmpty()) {
            return new SequenceNode(AstNode.Location.oneChar(tokens.getLine(), tokens.getColumn()));
        } else {
            var astNode = new SequenceNode(subNodes, naming);
            if (!exitStates.isEmpty()) {
                astNode.setParsingException(new ParsingException(
                        getIdentifier(),
                        "Chain parsed successful with allowed exceptions",
                        exitStates.toArray(ParsingException[]::new)
                ));
            }
            return astNode;
        }
    }

    private ParsingException softChainElementException(String elementName, ParsingException parsingException) {
        return new ParsingException(getIdentifier(), format(
                "Chain element '%s' parsed successfully with allowed exception",
                elementName
        ), parsingException);
    }


}
