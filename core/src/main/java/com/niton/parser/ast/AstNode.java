package com.niton.parser.ast;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Tokenizer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
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
public abstract class AstNode {
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
    public abstract Stream<Tokenizer.AssignedToken> join();

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

    public interface Location {
        default String format(){
            if(getFromLine() == getToLine()) {
                if (getFromColumn() == getToColumn())
                    return String.format("%d:%d", getFromLine(), getFromColumn());
                else
                    return String.format("%d:%d-%d", getFromLine(), getFromColumn(), getToColumn());
            }
            else
                return String.format("%d:%d-%d:%d", getFromLine(), getFromColumn(), getToLine(), getToColumn());
        }

        @NotNull
        static AstNode.Location of(int startLine, int startColumn, int endLine, int endColumn) {
            return new Location() {
                @Override
                public int getFromLine() {
                    return startLine;
                }

                @Override
                public int getFromColumn() {
                    return startColumn;
                }

                @Override
                public int getToLine() {
                    return endLine;
                }

                @Override
                public int getToColumn() {
                    return endColumn;
                }
            };
        }

        @NotNull
        static AstNode.Location range(Location from, Location to) {
            return of(from.getFromLine(), from.getFromColumn(), to.getToLine(), to.getToColumn());
        }

        static Location oneChar(int line, int column) {
           return of(line, column, line, column);
        }

        int getFromLine();

        int getFromColumn();

        int getToLine();

        int getToColumn();

        /**
         * Marks this position in the given text
         * by underlining it in the format of {@code ^------^} or {@code ^-^-> description} depending on the size of the description and the selected area
         *
         * @param text the text to mark this location in
         * @param context the number of lines to show before and after the marked area
         * @param description a description that describes what is marked. If no description is needed use {@code null}
         * @return the string containing the marked text
         */
        default String markInText(String text, int context, @Nullable String description) {
            //columns are human readable -> 1 based
            int fromLine = getFromLine()-1;
            int fromColumn = getFromColumn()-1;
            int toLine = getToLine()-1;
            int toColumn = getToColumn()-1;

            String[] lines = text.split("\n");
            StringBuilder builder = new StringBuilder();
            int startLine = Math.max(0, fromLine - context);
            int endLine = Math.min(lines.length, toLine + context);
            for (int i = startLine; i < endLine; i++) {
                String line = lines[i];
                builder.append(line).append("\n");
                if (i == fromLine) {
                    builder.append(repeat(" ",fromColumn)).append("^");
                    if (fromLine == toLine) {
                        builder.append(repeat("-",Math.max(0,toColumn - fromColumn - 2)));
                        if (toColumn-fromColumn > 1) builder.append("^");
                        if (description != null) {
                            builder.append("-> ").append(description);
                        }
                    } else {
                        builder.append(repeat("-",line.length() - fromColumn));
                    }
                    builder.append("\n");
                } else if(i > fromLine && i < toLine){
                    builder.append(repeat("-",line.length())).append("\n");
                } else if (i == toLine) {
                    builder.append(repeat("-",toColumn-1)).append("^");
                    if (description != null) {
                        builder.append("-> ").append(description);
                    }
                    builder.append("\n");
                }
            }
            return builder.toString();
        }
        private static String repeat(String s, int count) {//Tdoo teavm
            return String.join("", Collections.nCopies(count, s));
        }
    }
}

