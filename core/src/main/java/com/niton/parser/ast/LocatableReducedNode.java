package com.niton.parser.ast;

import com.niton.parser.token.Location;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class LocatableReducedNode extends ReducedNode{//This is a union type -> with java 17 use sealed class
    /**
     * only available if the node is a leaf
     */
    private final Location location;
    /**
     * @param location must be non-null if children is empty
     */
    LocatableReducedNode(
            String name,
            List<LocatableReducedNode> children,
            @Nullable Location location
    ) {
        super(name, new ArrayList<>(children));
        if(location == null && children.isEmpty())
            throw new IllegalArgumentException("A node without children must have a location");
        if (location != null) {
            this.location = location;
        } else {
            var firstChildLocation = children.get(0).getLocation();
            var lastChildLocation = children.get(children.size() - 1).getLocation();
            this.location = Location.of(
                    firstChildLocation.getFromLine(), firstChildLocation.getFromColumn(),
                    lastChildLocation.getToLine(), lastChildLocation.getToColumn()
            );
        }
    }

    LocatableReducedNode(String value, String name, Location location) {
        super(name, value);
        this.location = location;
    }

    /**
     * Creates a leaf node, a leave has no child nodes but a value instead
     *
     * @param name  the name of the resulting node
     * @param value the value of the node
     * @return the leaf node
     */
    public static LocatableReducedNode leaf(String name, String value, Location location) {
        if (value == null)
            throw new NullPointerException("Value of a leaf node can not be null");
        return new LocatableReducedNode(value, name, location);
    }

    /**
     * Creates a node that consists of sub-nodes
     *
     * @param name     the name of the resulting node
     * @param children the sub-nodes of this node 0..n
     * @return a node with children
     */
    public static LocatableReducedNode node(String name, List<LocatableReducedNode> children, @Nullable Location location) {
        return new LocatableReducedNode(name, children, location);
    }

    public Location getLocation() {
        return location;
    }


    /**
     * Get a sub node attached to this node. When the node is a leaf an exception is thrown
     *
     * @param name the name of the node to get
     * @return the node when a node with the regarding name exists, empty otherwise
     * @throws UnsupportedOperationException when using on a leaf
     */
    public Optional<LocatableReducedNode> getSubNode(String name) {
        return super.getSubNode(name).map(LocatableReducedNode.class::cast);
    }


    /**
     * Get all sub nodes attached to this node. When the node is a leaf an exception is thrown
     *
     * @return the list of sub-nodes
     * @throws UnsupportedOperationException when using on a leaf
     */
    public List<LocatableReducedNode> getChildren() {
        return super.getChildren().stream().map(LocatableReducedNode.class::cast).collect(toList());
    }
}

