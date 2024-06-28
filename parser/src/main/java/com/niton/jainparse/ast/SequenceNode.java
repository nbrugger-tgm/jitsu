package com.niton.jainparse.ast;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.Tokenable;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describes a node which contains an ordered sequence of other nodes.
 * This nodes <b>can</b> have a name associated with them.
 * Also named and unnamed nodes can be mixed.
 * <p>
 * When a node is named, it indicates the importance of interpretation and identification when processing the AST.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SequenceNode<T extends Enum<T> & Tokenable> extends AstNode<T> {
    private final Location explicitLocation;
    /**
     * The sub nodes of this node.
     */
    public List<AstNode<T>> subNodes;
    /**
     * The names of the sub nodes. Where the value is the index of the sub node in the list.
     */
    public Map<String, Integer> naming;

    public SequenceNode(List<AstNode<T>> subNodes, Map<String, Integer> naming) {
        this(getLocation(subNodes), Collections.unmodifiableList(subNodes), Collections.unmodifiableMap(naming));
    }

    public SequenceNode(Location explicitLocation) {
        this(explicitLocation, new ArrayList<>(), new LinkedHashMap<>());
    }

    @NotNull
    private static <T extends Enum<T> & Tokenable> Location getLocation(List<AstNode<T>> subNodes) {
        if (subNodes.isEmpty()) {
            throw new IllegalArgumentException("A sequence node must have at least one sub node or use the one arg constructor");
        }
        return Location.range(subNodes.get(0).getLocation(), subNodes.get(subNodes.size() - 1).getLocation());
    }

    /**
     * Appends a node to the end of the sub-node list and names it.
     *
     * @param name the name to be associated with the node
     * @param node the node to be added
     */
    public void name(String name, AstNode<T> node) {
        add(node);
        naming.put(name, subNodes.size() - 1);
    }

    /**
     * Appends an unnamed node to the end of the sub-node list. Retrieving an unnamed node is harder than a named one.
     *
     * @param node the node to be added
     * @return true if the node was added
     */
    public boolean add(AstNode<T> node) {
        return subNodes.add(node);
    }

    @Override
    public Location getLocation() {
        return explicitLocation;
    }

    public Stream<AssignedToken<T>> join() {
        return subNodes.stream()
                .flatMap(AstNode::join);
    }

    @Override
    public Optional<LocatableReducedNode> reduce(@NonNull String name) {
        if (subNodes.isEmpty())
            return Optional.of(LocatableReducedNode.node(name, List.of(), getLocation()));
        //When the children do not have a name, get children of the children to remove this layer of nesting,
        //since it holds no useful information (more than its children do)
        var namedSubnodes = !naming.isEmpty() ? getNamedProperties() : getDeepProperties();
        //when there are neither named nor deep (named) properties, create a leaf node
        if (namedSubnodes.isEmpty() && naming.isEmpty()) {
            return Optional.of(LocatableReducedNode.leaf(name, joinTokens(), getLocation()));
        }
        return Optional.of(LocatableReducedNode.node(name, namedSubnodes, getLocation()));
    }

    /**
     * @return a list that contains all named children of the (direct) children from this node
     */
    @NotNull
    private List<LocatableReducedNode> getDeepProperties() {
        return subNodes.stream()
                .flatMap(node -> node.reduce("no-name").stream())
                .filter(e -> !e.isLeaf())
                .map(LocatableReducedNode::getChildren)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * @return a list that contains all named children
     */
    @NotNull
    private List<LocatableReducedNode> getNamedProperties() {
        return naming.keySet().stream()
                .flatMap(key -> this.getNode(key).reduce(key).stream())
                .collect(Collectors.toList());
    }

    /**
     * Get a named sub-node by its name (added with {@link #name(String, AstNode)}).
     *
     * @param name the name of the sub object to find
     */
    public AstNode<T> getNode(String name) {
        return subNodes.get(naming.get(name));
    }

    /**
     * the toString of each sub node will be included in the output.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(getOriginGrammarName());
        for (AstNode<T> grammarObject : subNodes) {
            builder.append("\n   ");
            builder.append(grammarObject.toString().replace("\n", "\n   "));
        }
        builder.append("\n]");
        return builder.toString();
    }

    /**
     * Similar to {@link #toString()} but with a depth limit.
     */
    public String toString(int depth) {
        if (depth == 0) {
            return "[" + joinTokens() + "]";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append(getOriginGrammarName());
        for (AstNode<T> grammarObject : subNodes) {
            builder.append("\n   ");
            if (grammarObject instanceof SequenceNode) {
                builder.append(((SequenceNode<T>) grammarObject).toString(depth - 1)
                        .replaceAll("\n", "\n   "));
            } else {
                builder.append(grammarObject.toString().replaceAll("\n", "\n    "));
            }
        }
        builder.append("\n]");
        return builder.toString();
    }
}
