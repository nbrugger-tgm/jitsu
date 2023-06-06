package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.SequenceNode;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.TokenStream;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

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

    public RepeatMatcher(Grammar<?> expression) {
        this.check = expression;
    }


    /**
     * @param tokens
     * @param ref
     * @see GrammarMatcher#process(TokenStream, GrammarReference)
     */
    @Override
    public @NotNull SequenceNode process(@NotNull TokenStream tokens, @NotNull GrammarReference ref) throws ParsingException {
        SequenceNode obj = new SequenceNode();
        List<ParsingException> exitStates = new LinkedList<>();
        int resultIndex = 0;
        while (true) {
            try {
                var oldPos = tokens.index();
                AstNode gr = check.parse(tokens, ref);
                obj.name(String.valueOf(resultIndex++),gr);
                if (oldPos == tokens.index()) {
                    //Since the position did not change, the grammar did an empty match and the stream did not continue!
                    //This would lead to an infinite loop since it is a stalemate
                    exitStates.add(new ParsingException(getIdentifier(), format("The %s element parsed a zero-sized result",englishify(resultIndex)), gr.getParsingException()));
                    break;
                }
                if(gr.getParsingException() != null)
                    exitStates.add(new ParsingException(getIdentifier(), format("The %s element parsed successfull with exception",englishify(resultIndex)),gr.getParsingException()));
            } catch (ParsingException e) {
                exitStates.add(new ParsingException(getIdentifier(), format("The %s element failed and stopped parsing the repetition",englishify(resultIndex)), e));
                break;
            }
        }
        if(obj.subNodes.isEmpty())
            obj.setExplicitLocation(AstNode.Location.oneChar(tokens.getLine(), tokens.getColumn()));
        obj.setParsingException(new ParsingException(
                getIdentifier(),
                format("The repetition was parsed successful and matched %d elements before an terminating exception", resultIndex-1),
                exitStates.toArray(ParsingException[]::new)));
        return obj;
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
