package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer;
import org.jetbrains.annotations.NotNull;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AstNodeMocker {
	@NotNull
	public static AstNode getTokenNode(String grammarName, String tokenName, String tokenValue) {
		var tokenMock = mock(AstNode.class);
		when(tokenMock.getOriginGrammarName()).thenReturn(grammarName);
		when(tokenMock.join()).thenReturn(List.of(new Tokenizer.AssignedToken(
				tokenValue,
				tokenName
		)));
		when(tokenMock.reduce(any())).thenAnswer((Answer<ReducedNode>) invocation -> ReducedNode.leaf(
				invocation.getArguments()[0].toString(),
				tokenValue
		));
		return tokenMock;
	}

	public static AstNode getListNode(String grammarName, AstNode... subTokens) {
		var node = mock(AstNode.class);
		when(node.getOriginGrammarName()).thenReturn(grammarName);
		var joinedTokens = Arrays.stream(subTokens).map(AstNode::join)
		                         .flatMap(Collection::stream)
		                         .collect(Collectors.toList());
		when(node.join()).thenReturn(joinedTokens);
		AtomicInteger i = new AtomicInteger();
		when(node.reduce(any())).thenAnswer((Answer<ReducedNode>) invocation ->
				ReducedNode.node(
						invocation.getArguments()[0].toString(),
						Arrays.stream(subTokens)
						      .map((sub) -> sub.reduce(Integer.toString(i.getAndIncrement())))
						      .collect(Collectors.toList())
				)
		);
		return node;
	}

	public static AstNode getMockNode(String grammarName, ReducedNode someValue, String s) {
		var tokenMock = mock(AstNode.class);
		when(tokenMock.getOriginGrammarName()).thenReturn(grammarName);
		when(tokenMock.join()).thenReturn(List.of(new Tokenizer.AssignedToken(
				s,
				"MOCKED"
		)));
		when(tokenMock.reduce(any())).thenReturn(someValue);
		return tokenMock;
	}
}
