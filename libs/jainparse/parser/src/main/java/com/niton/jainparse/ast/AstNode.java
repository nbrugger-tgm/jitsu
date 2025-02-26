package com.niton.jainparse.ast;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.token.Tokenable;
import com.niton.jainparse.token.Tokenizer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * An AstNode is a node in the Abstract Syntax Tree (AST).
 * This is the base for any node in the tree and as such is not usefully creatable and only used for read access.
 * <p>
 * The purpose of this class is meant to represent the result of a parser on a text.
 * <p>
 * <b>It is heavily encouraged for parser-creators to produce out standard implementations and "composite" them together</b>
 * To create a new node use the implementations:
 * <ul>
 *     <li>{@link SequenceNode}: Represents a sequence of sub nodes that might or might not be named</li>
 *     <li>{@link SwitchNode}: Represents a node where a "choice" was made during parsing. Contains one sub node and the type which "choice" was taken</li>
 *     <li>{@link TokenNode}: Represents a sequence of tokens that were parsed</li>
 *     <li>{@link OptionalNode}: Represents a node that might or might not be present</li>
 * </ul>
 */
@Getter
@Setter
public abstract class AstNode<T extends Enum<T> & Tokenable> {
    public static final String ERRORNOUS_CONTENT = "$errornous_content";
    private String originGrammarName;
    /**
     * The parsing exception that occurred while parsing this node.
     * Even if a node parsed successfully an {@link ParsingException}  could occur during parsing that is "non-terminal"
     * Examples for such use cases are:
     * <ul>
     *     <li>ListGrammar: A list stops parsing when it encounters unparseable content - this parsing exception is stored here, but it is clear that a list of things will allways encounter an error somewhen.</li>
     *     <li>OptionalGrammar: When the content is not parsable that's ok - but still an exception occurred which is stored here</li>
     *     </ul>
     *     This list is not extensive.
     * <p>
     * This is needed to form a useful error message for the user when the parsing failed, and for developers to know what went wrong with their grammar.
     */
    private ParsingException parsingException;

    /**
     * The range of this node in the original text.
     */
    public abstract Location getLocation();

    /**
     * Joins all tokens of the underlying AST nodes recursively.
     * This leads to the original parsed text except of ignored tokens.
     */
    public String joinTokens() {
        StringBuilder builder = new StringBuilder();
        join().map(Tokenizer.AssignedToken::getValue).forEachOrdered(builder::append);
        return builder.toString();
    }

    /**
     * Collects all Tokens of underlying Grammars recursively. This leads to the original parsed
     * text except of ignored tokens
     *
     * @return the ordered stream of all recursive tokens
     */
    public abstract Stream<Tokenizer.AssignedToken<T>> join();

    /**
     * Reduces the tree to the bare minimum, usefully for interpreting the AST.
     * It gets rid of unnecessary layers, filters not needed nodes and condenses
     * AST nodes into strings when needed.
     * <p>
     * Example:
     * <pre>
     *    grammar    : repeat(any_grammar(chain_grammar(token(LETTER),token(NUMBER)))))
     *    parsed text: 123abc
     *    ast node   : repeat_node([switch_node(object_node(token_node("123"), toke_node("abc")))])
     * </pre>
     * The ast node is very bloated and hard to navigate since every grammar maps to exactly one node.
     * When reducing the node shown here the following is the result:
     * <pre>
     *    reduced : repeat_node(["123abc"])
     * </pre>
     * so reducing removes all intermediate steps to get to the value.
     * </p>
     * <p>
     * <b>Contract:</b></br>
     * The exact implementation is of course dependent on each node since "what you need for
     * interpreting the AST" is not a 100% clear definition.
     * <ul>
     *  <li>Named content is never discarded</li>
     *  <li>When nothing is relevant nothing can be returned ({@link Optional#empty()})</li>
     *  <li>You can proxy/forward other `reduce()` results</li>
     * </ul>
     * </p>
     *
     * @param name the name the returned node is given.
     *             This is important since retrieving nodes from the tree is done by their name,
     *             and it's always the parent node who defines the name of a child.
     *             The name can be a regular identifier or a numeric value (as string)
     * @return a reduced form of this node or {@link Optional#empty} when the node has no content worth interpreting
     */
    public abstract Optional<LocatableReducedNode> reduce(@NonNull String name);

    public boolean isError(){
        return originGrammarName.equals(ERRORNOUS_CONTENT);
    }
}

