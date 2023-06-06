package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer.AssignedToken;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
@RequiredArgsConstructor
@AllArgsConstructor
public class SequenceNode extends AstNode {
    /**
     * The sub nodes of this node.
     */
    public List<AstNode> subNodes = new ArrayList<>();
    /**
     * The names of the sub nodes. Where the value is the index of the sub node in the list.
     */
    public Map<String, Integer> naming = new LinkedHashMap<>();
    /**
     * The location of the node in the source code. Only required if the node has no sub nodes.
     */
    @Setter
    private @Nullable Location explicitLocation;


    /**
     * Appends a node to the end of the sub-node list and names it.
     *
     * @param name the name to be associated with the node
     * @param node the node to be added
     */
    public void name(String name, AstNode node) {
        add(node);
        naming.put(name, subNodes.size() - 1);
    }

    /**
     * Appends an unnamed node to the end of the sub-node list. Retrieving an unnamed node is harder than a named one.
     *
     * @param node the node to be added
     * @return
     */
    public boolean add(AstNode node) {
        return subNodes.add(node);
    }

    @Override
    public Location getLocation() {
        if (explicitLocation != null)
            return explicitLocation;
        return new Location() {
            @Override
            public int getFromLine() {
                return subNodes.get(0).getLocation().getFromLine();
            }

            @Override
            public int getFromColumn() {
                return subNodes.get(0).getLocation().getFromColumn();
            }

            @Override
            public int getToLine() {
                return subNodes.get(subNodes.size() - 1).getLocation().getToLine();
            }

            @Override
            public int getToColumn() {
                return subNodes.get(subNodes.size() - 1).getLocation().getToColumn();
            }
        };
    }

    public Stream<AssignedToken> join() {
        return subNodes.stream()
                .flatMap(AstNode::join);
    }

    @Override
    public Optional<LocatableReducedNode> reduce(@NonNull String name) {
        if(subNodes.size() == 0)
            return Optional.of(LocatableReducedNode.node(name, List.of(), getLocation()));
        //When the children do not have a name, get children of the children to remove this layer of nesting,
        //since it holds no useful information (more than its children do)
        var namedSubnodes = !naming.isEmpty() ? getNamedProperties() : getDeepProperties();
        //when there are neither named nor deep (named) properties, create a leaf node
        if (namedSubnodes.isEmpty() && naming.size() == 0) {
            namedSubnodes = List.of(LocatableReducedNode.leaf("value", joinTokens(), getLocation()));
        }
        return Optional.of(LocatableReducedNode.node(name, namedSubnodes,getLocation()));
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
    public AstNode getNode(String name) {
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
        for (AstNode grammarObject : subNodes) {
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
        for (AstNode grammarObject : subNodes) {
            builder.append("\n   ");
            if (grammarObject instanceof SequenceNode) {
                builder.append(((SequenceNode) grammarObject).toString(depth - 1)
                        .replaceAll("\n", "\n   "));
            } else {
                builder.append(grammarObject.toString().replaceAll("\n", "\n    "));
            }
        }
        builder.append("\n]");
        return builder.toString();
    }
}
