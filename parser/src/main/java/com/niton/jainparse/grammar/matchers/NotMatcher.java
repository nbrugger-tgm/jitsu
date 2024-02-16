package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.TokenStream;
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
