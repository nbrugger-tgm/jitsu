package com.niton.jainparse.grammar.matchers;

import com.niton.jainparse.ast.OptionalNode;
import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.api.GrammarMatcher;
import com.niton.jainparse.grammar.api.GrammarReference;
import com.niton.jainparse.token.TokenStream;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

/**
 * Cheks if the grammar is right if yes it adds the element to the output if not
 * it is ignored
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class OptionalMatcher extends GrammarMatcher<OptionalNode> {

    private Grammar<?> check;

    public OptionalMatcher(Grammar<?> value) {
        this.check = value;
    }


    /**
     * @param tokens
     * @param ref
     * @see GrammarMatcher#process(TokenStream, GrammarReference)
     */
    @Override
    public @NotNull ParsingResult<OptionalNode> process(@NotNull TokenStream tokens, @NotNull GrammarReference ref) {
        return ParsingResult.ok(
                check.parse(tokens, ref).map(
                        OptionalNode::new
                ).orElse((ex) -> new OptionalNode(tokens.currentLocation(), ex))
        );
    }

}
