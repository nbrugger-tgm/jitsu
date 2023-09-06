package com.niton.parser.grammar.matchers;

import com.niton.parser.ast.AstNode;
import com.niton.parser.ast.OptionalNode;
import com.niton.parser.ast.ParsingResult;
import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarMatcher;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.token.Location;
import com.niton.parser.token.TokenStream;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class NotMatcher extends GrammarMatcher<OptionalNode> {
    private final Grammar<?> grammarNotToMatch;

    @NotNull
    @Override
    protected ParsingResult<OptionalNode> process(@NotNull TokenStream tokens, @NotNull GrammarReference reference)  {
        tokens.elevate();
        var start = tokens.currentLocation();
        var result = grammarNotToMatch.parse(tokens, reference);
        if(!result.wasParsed()){
            tokens.commit();
            var node = new OptionalNode(
                    tokens.currentLocation(),
                    new ParsingException(getIdentifier(), "Successfully  mis-matched " + grammarNotToMatch.getIdentifier(), result.exception())
            );
            return ParsingResult.ok(node);
        }
        AstNode node = result.unwrap();
        var exception = node.getParsingException();
        tokens.rollback();
        if (exception != null)
            return ParsingResult.error(new ParsingException(getIdentifier(), "Expected not to match " + grammarNotToMatch.getIdentifier(), exception));
        else return ParsingResult.error(new ParsingException(
                getIdentifier(),
                "Expected not to match " + grammarNotToMatch.getIdentifier(),
                Location.range(start, tokens.currentLocation())
        ));
    }
}
