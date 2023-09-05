package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.ParsingResult;
import com.niton.parser.ast.SequenceNode;
import com.niton.parser.ast.TokenNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.types.AnyExceptGrammar;
import com.niton.parser.grammar.types.ChainGrammar;
import com.niton.parser.token.Location;
import com.niton.parser.token.TokenStream;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.niton.parser.ast.AstNode.ERRORNOUS_CONTENT;
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
    public @NotNull ParsingResult<SequenceNode> process(
            @NotNull TokenStream tokens,
            @NotNull GrammarReference reference
    ) {
        List<ParsingException> exitStates = new ArrayList<>(chain.getChain().size());
        Map<String, Integer> naming = new HashMap<>();
        List<AstNode> subNodes = new ArrayList<>();
        int i = 0;
        if (recursionMarkers.stream().anyMatch(marker -> matchesMarker(tokens, marker))) {
            return ParsingResult.error(
                    new ParsingException(getIdentifier(), "Left recursion occured!", tokens.currentLocation())
            );
        }
        if (chain.isLeftRecursive() && firstNodeSubstitute == null) {
            var thisMarker = new RecursionMarker(chain, tokens.index());
            recursionMarkers.add(thisMarker);
            var result = substituteLeftRecursion(tokens, reference);
            recursionMarkers.removeIf(marker -> marker == thisMarker);
            return result;
        }
        for (var grammar : chain.getChain()) {
            ParsingResult<? extends AstNode> res;
            if (i == 0 && firstNodeSubstitute != null) {
                res = ParsingResult.ok(firstNodeSubstitute);
            } else {
                GrammarMatcher.additionalInfo = chain.getNaming().get(i);
                if(GrammarMatcher.additionalInfo == null)
                    GrammarMatcher.additionalInfo = "";
                res = grammar.parse(tokens, reference);
            }
            if(!res.wasParsed()) {
                var elementName = chain.getNaming().get(i);
                var crashingException = new ParsingException(
                        getIdentifier(),
                        chain.getIdentifier()+"  entry '"+(elementName != null ? elementName : grammar)+"' can't be parsed!",
                        res.exception()
                );
                exitStates.add(crashingException);
                if(i+1 == chain.getChain().size() || i == 0) {
                    return ParsingResult.error(new ParsingException(getIdentifier(),chain.getIdentifier()+"  could not be parsed!", exitStates.toArray(ParsingException[]::new)));
                }else {
//                    Grammar<?> nextGrammar = chain.getChain().get(i + 1);
//                    var preUnknownContentIndex = tokens.index();
//                    var unknownIntermediateContent = new AnyExceptMatcher(nextGrammar).parse(tokens, reference);
//                    i++;
//                    if(!unknownIntermediateContent.wasParsed() || preUnknownContentIndex == tokens.index())
                        return ParsingResult.error(new ParsingException(getIdentifier(), chain.getIdentifier()+"  could not be parsed!", exitStates.toArray(ParsingException[]::new)));
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
            return ParsingResult.ok(new SequenceNode(Location.oneChar(tokens.getLine(), tokens.getColumn())));
        } else {
            var astNode = new SequenceNode(subNodes, naming);
            if (!exitStates.isEmpty()) {
                astNode.setParsingException(new ParsingException(
                        getIdentifier(),
                        chain.getIdentifier()+" parsed successful with allowed exceptions",
                        exitStates.toArray(ParsingException[]::new)
                ));
            }
            return ParsingResult.ok(astNode);
        }
    }

    private ParsingResult<SequenceNode> substituteLeftRecursion(TokenStream tokens, GrammarReference reference) {
        var leftElement = chain.getChain().get(0).parse(tokens, reference);
        if (!leftElement.wasParsed()) {
            return ParsingResult.error(new ParsingException(getIdentifier(), "Couldn't parse first element of"+ chain.getIdentifier() , leftElement.exception()));
        }
        var leftNode = leftElement.unwrap();
        var subMatcher = chain.createExecutor();
        var matchedAny = false;
        while (true) {
            subMatcher.firstNodeSubstitute = leftElement.unwrap();
            var newSubst = subMatcher.parse(tokens, reference);
            if (!newSubst.wasParsed()) {
                var oldExceptions = leftNode.getParsingException();
                leftNode.setParsingException(new ParsingException(
                        getIdentifier(),
                        chain.getIdentifier()+" parsed successful with allowed exceptions",
                        oldExceptions == null ?
                                new ParsingException[]{newSubst.exception()} :
                                new ParsingException[]{newSubst.exception(), oldExceptions}
                ));
                break;
            }
            leftNode = newSubst.unwrap();
            matchedAny = true;
        }
        if (!matchedAny) {
            return ParsingResult.error(leftNode.getParsingException());
        } else {
            return ParsingResult.ok((SequenceNode) leftNode);
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
