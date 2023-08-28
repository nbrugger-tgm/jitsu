package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.SequenceNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.Location;
import com.niton.parser.token.TokenStream;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

/**
 * Checks the grammar as often as is ocures
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class RepeatMatcher extends GrammarMatcher<SequenceNode> {

    private Grammar<?> check;
    private final int minimum;

    public RepeatMatcher(Grammar<?> expression, int minimum) {
        this.check = expression;
        this.minimum = minimum;
    }


    @Override
    public @NotNull SequenceNode process(@NotNull TokenStream tokens, @NotNull GrammarReference ref) throws ParsingException {
        List<ParsingException> exitStates = new LinkedList<>();
        List<AstNode> subNodes = new ArrayList<>(4);
        int resultIndex = 0;
        while (true) {
            try {
                var oldPos = tokens.index();
                AstNode subNode = check.parse(tokens, ref);
                subNodes.add(subNode);
                if (oldPos == tokens.index()) {
                    //Since the position did not change, the grammar did an empty match and the stream did not continue!
                    //This would lead to an infinite loop since it is a stalemate
                    exitStates.add(new ParsingException(getIdentifier(), format("The %s element parsed a zero-sized result",englishify(resultIndex)), subNode.getParsingException()));
                    break;
                }
                if(subNode.getParsingException() != null)
                    exitStates.add(new ParsingException(getIdentifier(), format("The %s element parsed successfull with exception",englishify(resultIndex)),subNode.getParsingException()));
            } catch (ParsingException e) {
                exitStates.add(new ParsingException(getIdentifier(), format("The %s element failed and stopped parsing the repetition",englishify(resultIndex)), e));
                break;
            }
            resultIndex++;
        }
        if(resultIndex < minimum) {
            throw new ParsingException(getIdentifier(), format("The repetition only matched %d from the minimum of %d elements", resultIndex, minimum), exitStates.toArray(ParsingException[]::new));
        }
        SequenceNode astNode;
        if(subNodes.isEmpty()) {
            astNode = new SequenceNode(Location.oneChar(tokens.getLine(), tokens.getColumn()));
        } else {
            astNode = new SequenceNode(subNodes, IntStream.range(0, subNodes.size()).boxed().collect(toMap(Object::toString, i -> i)));
        }
        astNode.setParsingException(new ParsingException(
                getIdentifier(),
                format("The repetition was parsed successful and matched %d elements before an terminating exception", resultIndex-1),
                exitStates.toArray(ParsingException[]::new)));
        return astNode;
    }

    private String englishify(int resultIndex) {
        switch (resultIndex) {
            case 1:
                return "first";
            case 2:
                return "second";
            case 3:
                return "third";
            default:
                return format("%dth", resultIndex);
        }
    }
}
