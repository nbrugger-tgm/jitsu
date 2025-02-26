package com.niton.parsers.grammar;

import com.niton.jainparse.ast.AstNode;
import com.niton.jainparse.ast.ReducedNode;
import com.niton.jainparse.grammar.api.Grammar;
import com.niton.jainparse.grammar.types.AnyExceptGrammar;
import com.niton.jainparse.grammar.types.ChainGrammar;
import com.niton.jainparse.parser.Parser;
import com.niton.jainparse.token.Tokenable;
import com.niton.jainparse.token.Tokenizer;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.niton.parsers.grammar.GrammarFileGrammar.GRAMMAR_FILE_GRAMMAR;
import static com.niton.parsers.grammar.GrammarFileGrammar.Property.*;
import static com.niton.parsers.grammar.GrammarFileGrammarName.*;
import static com.niton.parsers.grammar.GrammarFileTokens.*;

/**
 * Parser for GrammarFiles
 *
 * @author Nils Brugger
 * @version 2019-06-09
 */
public class GrammarFileParser extends Parser<GrammarFileContent, GrammarFileTokens> {

    public GrammarFileParser() {
        super(GRAMMAR_FILE_GRAMMAR);
        setTokenizer(new Tokenizer<>(GrammarFileTokens.values()));
    }

    /**
     * @see com.niton.jainparse.Parser#convert(AstNode)
     */
    @Override
    public @NotNull GrammarFileContent convert(@NotNull AstNode<GrammarFileTokens> o) {
        var root = o.reduce("GrammarFile").orElseThrow(() -> new RuntimeException("No root node"));
        var head = root.getSubNode(HEAD.id());
        var result = new GrammarFileContent();
        var tokens = head.flatMap(node -> node.getSubNode(TOKEN_DEFINERS.id()))
                .map(ReducedNode::getChildren)
                .stream()
                .flatMap(List::stream)
                .map(this::parseTokenDefiner)
                .collect(Collectors.toSet());
        result.setTokens(tokens);

        root.getSubNode(GRAMMARS.id())
                .map(ReducedNode::getChildren)
                .stream()
                .flatMap(List::stream)
                .map(this::parseChainGrammar)
                .forEach(result.getGrammars()::map);
        return result;
    }

    private Grammar<?,GrammarFileTokens> parseChainGrammar(ReducedNode rootGrammarNode) {
        var name = rootGrammarNode.getSubNode(NAME.id())
                .map(ReducedNode::getValue)
                .orElseThrow(() -> new RuntimeException("Grammar has no name"));
        final var grammars = rootGrammarNode.getSubNode(CHAIN.id())
                .map(ReducedNode::getChildren)
                .stream()
                .flatMap(List::stream)
                .map(this::parseGenericGrammar);
        var chain = new ChainGrammar<GrammarFileTokens>();
        grammars.forEach(elem -> {
            if (elem.getName() != null) {
                chain.addGrammar(elem, elem.getName());
            }
            chain.addGrammar(elem);
        });
        return chain.named(name);
    }

    private Grammar<AstNode<GrammarFileTokens>,GrammarFileTokens> parseGenericGrammar(ReducedNode grammarDefinition) {
        var operator = grammarDefinition.getSubNode(OPERATION.id());
        var item = grammarDefinition.getSubNode(ITEM.id()).orElseThrow(
                () -> new RuntimeException("No item for grammar")
        );
        var repeat = grammarDefinition.getSubNode(REPEAT.id());
        var assignment = grammarDefinition.getSubNode(ASSIGNMENT.id());
        Grammar<?,GrammarFileTokens> grammar = parseItem(item);
        if (operator.isPresent()) {
            var operatorValue = operator.get().getValue();
            if (operatorValue.matches(ANY_EXCEPT_SIGN.regex))
                grammar = new AnyExceptGrammar<>(grammar);
            else if (operatorValue.matches(OPTIONAL_SIGN.regex))
                grammar = grammar.optional();
            else if (operatorValue.matches(IGNORE_SIGN.regex))
                grammar = grammar.ignore();
        }
        if (repeat.isPresent()) {
            grammar = grammar.repeat();
        }
        if (assignment.isPresent()) {
            var name = assignment.get().getSubNode(NAME.id()).orElseThrow().getValue();
            grammar = grammar.named(name);
        }
        return (Grammar<AstNode<GrammarFileTokens>,GrammarFileTokens>) grammar;
    }

    private Grammar<?,?> parseItem(ReducedNode item) {
        var type = item.getSubNode("type").orElseThrow().getValue();
        var possibleReference = parseReferenceType(item, type);
        if (possibleReference.isPresent())
            return possibleReference.get();
        if (type.equals(ARRAY.name())) {
            return parseArrayGrammar(item.getSubNode("value").orElseThrow());
        } else {
            throw new IllegalArgumentException(type + " is not a known item type");
        }
    }

    private Optional<Grammar<?,?>> parseReferenceType(ReducedNode item, String type) {
        if (type.equals(TOKEN_REFERENCE.name())) {
            return Optional.of(parseTokenReference(item.getSubNode("value").orElseThrow()));
        }
        if (type.equals(GRAMMAR_REFERENCE.name())) {
            return Optional.of(
                    Grammar.createReference(item.getSubNode("value").orElseThrow().getValue())
            );
        }
        return Optional.empty();
    }

    private<RT extends Enum<RT> & Tokenable> Grammar<?,?> parseArrayGrammar(ReducedNode value) {
        var refs = value
                .getSubNode(ITEMS.id())
                .map(ReducedNode::getChildren)
                .stream()
                .flatMap(List::stream)
                .map(item -> item.getSubNode(REFERENCE.id()))
                .map(Optional::orElseThrow)
                .map(ref -> {
                    var type = ref.getSubNode("type").orElseThrow().getValue();
                    return parseReferenceType(ref, type).orElseThrow(
                            () -> new IllegalArgumentException(type + " is not a known item type")
                    );
                })
                .toArray(Grammar[]::new);
        return Grammar.<RT>anyOf(refs);
    }

    private Grammar<?,?> parseTokenReference(ReducedNode value) {
        return Grammar.token(value.getSubNode(TOKEN_NAME.id()).orElseThrow().getValue());
    }

    private Tokenable parseTokenDefiner(ReducedNode reducedNode) {
        var regex = reducedNode.getSubNode(LITERAL.id())
                .flatMap(literal -> literal.getSubNode(REGEX.id()))
                .map(ReducedNode::join)
                .orElseThrow(() -> new RuntimeException("No regex in token"));
        return () -> regex;
    }

}

