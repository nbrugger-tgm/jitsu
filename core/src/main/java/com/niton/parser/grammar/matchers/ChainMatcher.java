package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.SequenceNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.ChainGrammar;
import com.niton.parser.token.Location;
import com.niton.parser.token.TokenStream;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

    private static final Set<RecursionMarker> recursionMarkers = new HashSet<>();
    private final ChainGrammar chain;
    private @Nullable AstNode firstNodeSubstitute;

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
        if (recursionMarkers.stream().anyMatch(marker -> matchesMarker(tokens, marker))) {
            throw new ParsingException(getIdentifier(), "Left recursion occured!", tokens.currentLocation());
        }
        if (chain.isLeftRecursive() && firstNodeSubstitute == null) {
            var thisMarker = new RecursionMarker(chain, tokens.index());
            recursionMarkers.add(thisMarker);
            var result = substituteLeftRecursion(tokens, reference);
            recursionMarkers.removeIf(marker -> marker == thisMarker);
            return result;
        }
        for (var grammar : chain.getChain()) {
            try {
                AstNode res;
                if (i == 0 && firstNodeSubstitute != null) {
                    res = firstNodeSubstitute;
                } else {
                    res = grammar.parse(tokens, reference);
                }
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
            return new SequenceNode(Location.oneChar(tokens.getLine(), tokens.getColumn()));
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

    private SequenceNode substituteLeftRecursion(TokenStream tokens, GrammarReference reference) throws ParsingException {
        try {
            var firstSubstitution = chain.getChain().get(0).parse(tokens, reference);
            var subMatcher = chain.createExecutor();
            var matchedAny = false;
            while (true) {
                try {
                    subMatcher.firstNodeSubstitute = firstSubstitution;
                    firstSubstitution = subMatcher.parse(tokens, reference);
                    matchedAny = true;
                } catch (ParsingException e) {
                    var oldExceptions = firstSubstitution.getParsingException();
                    firstSubstitution.setParsingException(new ParsingException(
                            getIdentifier(),
                            "Chain parsed successful with allowed exceptions",
                            oldExceptions == null ?
                                    new ParsingException[]{e} :
                                    new ParsingException[]{e, oldExceptions}
                    ));
                    break;
                }
            }
            if(!matchedAny && firstSubstitution.getParsingException() != null) {
                throw firstSubstitution.getParsingException();
            } else {
                return (SequenceNode) firstSubstitution;
            }
        } catch (ParsingException e) {
            throw new ParsingException(getIdentifier(), "Couldn't parse first element of chain", e);
        }
    }

    private boolean matchesMarker(@NotNull TokenStream tokens, RecursionMarker marker) {
        return marker.getGrammar() == chain && marker.getIndex() == tokens.index();
    }

    private ParsingException softChainElementException(String elementName, ParsingException parsingException) {
        return new ParsingException(getIdentifier(), format(
                "Chain element '%s' parsed successfully with allowed exception",
                elementName
        ), parsingException);
    }

    @Data
    private static class RecursionMarker {
        private final ChainGrammar grammar;
        private final int index;
    }


}
