package com.niton.jainparse.ast;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.Tokenizer;
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
        when(tokenMock.joinTokens()).thenReturn(tokenValue);
        when(tokenMock.reduce(any())).thenAnswer(invocation -> Optional.of(LocatableReducedNode.leaf(
                invocation.getArguments()[0].toString(),
                tokenValue,
                Location.oneChar(0, tokenValue.length())
        )));
        when(tokenMock.getLocation()).thenReturn(Location.oneChar(0, tokenValue.length()));
        return tokenMock;
    }

    public static AstNode getListNode(String grammarName, AstNode... subTokens) {
        var node = mock(AstNode.class);
        when(node.getOriginGrammarName()).thenReturn(grammarName);
        var joinedTokens = Arrays.stream(subTokens)
                .flatMap(AstNode::join)
                .collect(Collectors.toList());
        when(node.join()).thenAnswer(i -> joinedTokens.stream());
        when(node.joinTokens()).thenAnswer(i -> Arrays.stream(subTokens).map(AstNode::joinTokens).collect(Collectors.joining()));
        AtomicInteger i = new AtomicInteger();
        when(node.reduce(any())).thenAnswer(invocation -> Optional.of(LocatableReducedNode.node(
                invocation.getArguments()[0].toString(),
                Arrays.stream(subTokens)
                        .flatMap((sub) -> sub.reduce(Integer.toString(i.getAndIncrement())).stream())
                        .collect(Collectors.toList()),
                Location.oneChar(
                        0,
                        Arrays.stream(subTokens)
                                .map(tok -> tok.joinTokens().length()).reduce(0, Integer::sum)
                )
        )));
        return node;
    }

    public static AstNode getMockNode(String grammarName, LocatableReducedNode reduced, String s) {
        var tokenMock = mock(AstNode.class);
        when(tokenMock.getOriginGrammarName()).thenReturn(grammarName);
        when(tokenMock.join()).thenAnswer(i -> Stream.of(new Tokenizer.AssignedToken(s, "MOCKED")));
        when(tokenMock.joinTokens()).thenReturn(s);
        when(tokenMock.reduce(any())).thenReturn(Optional.ofNullable(reduced));
        return tokenMock;
    }
}
