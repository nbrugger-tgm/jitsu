package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AstNodeMocker {
    @NotNull
    public static AstNode getTokenNode(String grammarName, String tokenName, String tokenValue) {
        var tokenMock = mock(AstNode.class);
        when(tokenMock.getOriginGrammarName()).thenReturn(grammarName);
        when(tokenMock.join()).thenAnswer(i -> Stream.of(new Tokenizer.AssignedToken(tokenValue, tokenName)));
        when(tokenMock.reduce(any())).thenAnswer(invocation -> Optional.of(ReducedNode.leaf(
                invocation.getArguments()[0].toString(),
                tokenValue
        )));
        return tokenMock;
    }

    public static AstNode getListNode(String grammarName, AstNode... subTokens) {
        var node = mock(AstNode.class);
        when(node.getOriginGrammarName()).thenReturn(grammarName);
        var joinedTokens = Arrays.stream(subTokens)
                .flatMap(AstNode::join)
                .collect(Collectors.toList());
        when(node.join()).thenAnswer(i -> joinedTokens.stream());
        AtomicInteger i = new AtomicInteger();
        when(node.reduce(any())).thenAnswer(invocation -> Optional.of(ReducedNode.node(
                invocation.getArguments()[0].toString(),
                Arrays.stream(subTokens)
                        .flatMap((sub) -> sub.reduce(Integer.toString(i.getAndIncrement())).stream())
                        .collect(Collectors.toList())
        )));
        return node;
    }

    public static AstNode getMockNode(String grammarName, ReducedNode someValue, String s) {
        var tokenMock = mock(AstNode.class);
        when(tokenMock.getOriginGrammarName()).thenReturn(grammarName);
        when(tokenMock.join()).thenAnswer(i ->Stream.of(new Tokenizer.AssignedToken(s, "MOCKED")));
        when(tokenMock.reduce(any())).thenReturn(Optional.ofNullable(someValue));
        return tokenMock;
    }
}
