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
import com.niton.jainparse.token.Tokenable;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class NotMatcher<T extends Enum<T> & Tokenable> extends GrammarMatcher<OptionalNode<T>,T> {
    private final Grammar<?,T> grammarNotToMatch;

    @NotNull
    @Override
    protected ParsingResult<OptionalNode<T>> process(@NotNull TokenStream<T> tokens, @NotNull GrammarReference<T> reference)  {
        tokens.elevate();
        var start = tokens.currentLocation();
        var result = grammarNotToMatch.parse(tokens, reference);
        if(!result.wasParsed()){
            tokens.commit();
            var node = new OptionalNode<T>(
                    tokens.currentLocation(),
                    new ParsingException(getIdentifier(), "Successfully  mis-matched " + grammarNotToMatch.getIdentifier(), result.exception())
            );
            return ParsingResult.ok(node);
        }
        AstNode<T> node = result.unwrap();
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
