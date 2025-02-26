package com.niton.jainparse.ast;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.Tokenable;
import com.niton.jainparse.token.Tokenizer;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This is the node that is produced if a grammar can match different grammars. This node expresses which of the grammars matched.
 * For example, the grammar: {@code {TEXT or NUMBER}} would produce a switch node showing {@code TEXT} or {@code NUMBER} was matched and also the matched value itself.
 */
public class SwitchNode<T extends Enum<T> & Tokenable> extends AstNode<T> {
    private final AstNode<T> result;

    public SwitchNode(AstNode<T> res) {
        this.result = res;
    }

    @Override
    public Location getLocation() {
        return result.getLocation();
    }

    /**
     * @return the type which shows which grammar of the options was matched. Can be used to determine how to interpret the {@link #getResult()}.
     */
    public String getType() {
        return result.getOriginGrammarName();
    }

    @Override
    public Stream<Tokenizer.AssignedToken<T>> join() {
        return result.join();
    }

    @Override
    public Optional<LocatableReducedNode> reduce(@NonNull String name) {
        if (getType() != null) {
            var innerNode = result.reduce("value");
            return innerNode.map(node -> LocatableReducedNode.node(name, List.of(
                    LocatableReducedNode.leaf("type", getType(), node.getLocation()),
                    node
            ), node.getLocation()));
        } else {
            return result.reduce(name);
        }
    }

    /**
     * @return the result of the match
     */
    public AstNode<T> getResult() {
        return result;
    }

    @Override
    public String toString() {
        return result.toString();
    }
}

