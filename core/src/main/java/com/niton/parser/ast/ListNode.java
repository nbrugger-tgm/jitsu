package com.niton.parser.ast;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Tokenizer;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListNode extends AstNode {
    private List<AstNode> list;

    public ListNode() {
        this(new ArrayList<>());
    }

    public ListNode(List<AstNode> nodes) {
        list = nodes;
    }

    public void clear() {
        list.clear();
    }

    public AstNode get(int index) {
        return list.get(index);
    }

    public void add(AstNode element) {
        list.add(element);
    }

    public AstNode remove(int index) {
        return list.remove(index);
    }

    public List<AstNode> getList() {
        return list;
    }

    public void setList(List<AstNode> list) {
        this.list = list;
    }

    @Override
    public Collection<Tokenizer.AssignedToken> join() {
        if (list.isEmpty()) {
            return new ArrayList<>(0);
        }
        return list.stream()
                .map(AstNode::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public ReducedNode reduce(@NonNull String name) {
        int i = 0;
        List<ReducedNode> reduced = new ArrayList<>();
        for (AstNode node : list) {
            var reducedChild = node.reduce(Integer.toString(i++));
            if (reducedChild != null)
                reduced.add(reducedChild);
        }
        return ReducedNode.node(name, reduced);
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("[").append(getOriginGrammarName()).append("\n");
        for (AstNode grammarResult : list) {
            bldr.append("\t");
            bldr.append(grammarResult.toString());
        }
        bldr.append("]\n");

        return bldr.toString();
    }

    @Override
    public ParsingException getParsingException() {
        var exceptions = Stream.concat(
                list.stream()
                        .map(AstNode::getParsingException)
                        .filter(Objects::nonNull),
                Optional.ofNullable(super.getParsingException()).stream()
        ).collect(Collectors.toUnmodifiableList());
        if (super.getParsingException() == null && !exceptions.isEmpty())
            return new ParsingException(getOriginGrammarName(), "List parsed successfully", exceptions.toArray(ParsingException[]::new));
        return super.getParsingException();
    }
}
