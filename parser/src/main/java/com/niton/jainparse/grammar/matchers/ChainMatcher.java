package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.ast.SequenceNode;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.grammar.types.ChainGrammar;
import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.TokenStream;
import com.niton.jainparse.token.Tokenable;
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
public class ChainMatcher<T extends Enum<T> & Tokenable> extends GrammarMatcher<SequenceNode<T>,T> {

    private static final Set<RecursionMarker> recursionMarkers = new HashSet<>();
    private final ChainGrammar<T> chain;

    public ChainMatcher(ChainGrammar<T> chain) {
        this.chain = chain;
        setOriginGrammarName(chain.getName());
        setIdentifier(chain.getIdentifier());
    }

    @Override
    public @NotNull ParsingResult<SequenceNode<T>> process(
            @NotNull TokenStream<T> tokens,
            @NotNull GrammarReference<T> reference
    ) {
        List<ParsingException> exitStates = new ArrayList<>(chain.getChain().size());
        Map<String, Integer> naming = new HashMap<>();
        List<AstNode<T>> subNodes = new ArrayList<>();
        int i = 0;
        var index = tokens.index();
        var optRecursionMarker = recursionMarkers.stream().filter(m -> m.index == index).findFirst();
        if (chain.isLeftRecursive() && optRecursionMarker.isPresent() && optRecursionMarker.get().firstNodeSubstitute == null) {
            return ParsingResult.error(
                    new ParsingException(getIdentifier(), "Left recursion occured!", tokens.currentLocation())
            );
        } else if (!chain.isLeftRecursive() && optRecursionMarker.isPresent() && optRecursionMarker.get().firstNodeSubstitute != null) {
            return ParsingResult.error(new ParsingException(
                    getIdentifier(),
                    "Skipping non-recursive node when resolving left-recursion",
                    tokens.currentLocation()
            ));
        }
        if (chain.isLeftRecursive() && optRecursionMarker.isEmpty()) {
            var thisMarker = new RecursionMarker(chain);
            thisMarker.index = index;
            recursionMarkers.add(thisMarker);
            var result = substituteLeftRecursion(tokens, reference, thisMarker);
            recursionMarkers.removeIf(marker -> marker == thisMarker);
            return result;
        }
        var recursionMarker = optRecursionMarker.orElse(null);
        for (var grammar : chain.getChain()) {
            ParsingResult<? extends AstNode<T>> res;
            if (i == 0 && (chain.isLeftRecursive() && recursionMarker.firstNodeSubstitute != null)) {
                res = ParsingResult.ok(recursionMarker.firstNodeSubstitute);
            } else {
                GrammarMatcher.additionalInfo = chain.getNaming().get(i);
                if (GrammarMatcher.additionalInfo == null)
                    GrammarMatcher.additionalInfo = "";
                res = grammar.parse(tokens, reference);
            }
            if (!res.wasParsed()) {
                var elementName = chain.getNaming().get(i);
                var crashingException = new ParsingException(
                        getIdentifier(),
                        chain.getIdentifier() + "  entry '" + (elementName != null ? elementName : grammar) + "' can't be parsed!",
                        res.exception()
                );
                exitStates.add(crashingException);
                if (i + 1 == chain.getChain().size() || i == 0) {
                    return ParsingResult.error(new ParsingException(getIdentifier(), chain.getIdentifier() + "  could not be parsed!", exitStates.toArray(ParsingException[]::new)));
                } else {
//                    Grammar<?> nextGrammar = chain.getChain().get(i + 1);
//                    var preUnknownContentIndex = tokens.index();
//                    var unknownIntermediateContent = new AnyExceptMatcher(nextGrammar).parse(tokens, reference);
//                    i++;
//                    if(!unknownIntermediateContent.wasParsed() || preUnknownContentIndex == tokens.index())
                    return ParsingResult.error(new ParsingException(getIdentifier(), chain.getIdentifier() + "  could not be parsed!", exitStates.toArray(ParsingException[]::new)));
//                    var errornousContent = unknownIntermediateContent.unwrap();
//                    errornousContent.setOriginGrammarName(ERRORNOUS_CONTENT);
//                    subNodes.add(errornousContent);
//                    continue;
                }
            }
            subNodes.add(res.unwrap());
            String name;
            if ((name = chain.getNaming().get(i)) != null) {
                naming.put(name, i);
            }
            var softException = res.unwrap().getParsingException();
            if (softException != null)
                exitStates.add(softChainElementException(name != null ? name : grammar.getIdentifier(), softException));
            i++;
        }
        if (subNodes.isEmpty()) {
            var astNode = new SequenceNode(Location.oneChar(tokens.getLine(), tokens.getColumn()));
            optRecursionMarker.ifPresent(marker -> {
                if (marker.root == chain) marker.lastSameTypeNode = astNode;
            });
            return ParsingResult.ok(astNode);
        } else {
            var astNode = new SequenceNode(subNodes, naming);
            if (!exitStates.isEmpty()) {
                astNode.setParsingException(new ParsingException(
                        getIdentifier(),
                        chain.getIdentifier() + " parsed successful with allowed exceptions",
                        exitStates.toArray(ParsingException[]::new)
                ));
            }
            optRecursionMarker.ifPresent(marker -> {
                if (marker.root == chain) marker.lastSameTypeNode = astNode;
            });
            return ParsingResult.ok(astNode);
        }
    }

    private ParsingResult<SequenceNode<T>> substituteLeftRecursion(
            TokenStream<T> tokens,
            GrammarReference<T> reference,
            RecursionMarker recursionMarker
    ) {
        Grammar<?,T> leftmostGrammar = chain.getChain().get(0);
        var leftElement = leftmostGrammar.parse(tokens, reference);
        if (!leftElement.wasParsed()) {
            return ParsingResult.error(new ParsingException(getIdentifier(), "Couldn't parse first element of" + chain.getIdentifier(), leftElement.exception()));
        }
        var leftNode = leftElement.unwrap();
        while (true) {
            recursionMarker.index = tokens.index();
            recursionMarker.firstNodeSubstitute = leftNode;
            var newSubst = leftmostGrammar.parse(tokens, reference);
            if (!newSubst.wasParsed()) {
                var oldExceptions = leftNode.getParsingException();
                leftNode.setParsingException(new ParsingException(
                        getIdentifier(),
                        chain.getIdentifier() + " parsed successful with allowed exceptions",
                        oldExceptions == null ?
                                new ParsingException[]{newSubst.exception()} :
                                new ParsingException[]{newSubst.exception(), oldExceptions}
                ));
                break;
            }
            leftNode = newSubst.unwrap();
        }
        var last = recursionMarker.lastSameTypeNode;
        if (last == null) {
            return ParsingResult.error(leftNode.getParsingException());
        } else {
            return ParsingResult.ok(last);
        }
    }

    private ParsingException softChainElementException(String elementName, ParsingException parsingException) {
        return new ParsingException(getIdentifier(), format(
                "Chain element '%s' parsed successfully with allowed exception",
                elementName
        ), parsingException);
    }

    @Data
    private static class RecursionMarker {
        private final ChainGrammar<?> root;
        private @Nullable AstNode<?> firstNodeSubstitute;
        private int index;
        private @Nullable SequenceNode<?> lastSameTypeNode;
    }


}
