package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer.AssignedToken;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.niton.parser.ast.AstNodeMocker.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnyNodeTest extends AstNodeTest<AstNode> {

	@Test
	void join() {
		var subNode = mock(AstNode.class);
		var node    = new SwitchNode(subNode);

		var subNodeToken = Stream.of(new AssignedToken("a", "b"));
		when(subNode.join()).thenReturn(subNodeToken);
		var joined = node.join();

		assertThat(joined).isSameAs(subNodeToken);
	}

	@Test
	void reduceWithTypeLeaf() {
		var subNode = mock(AstNode.class);
		when(subNode.getOriginGrammarName()).thenReturn("grammar-a");
		when(subNode.reduce("value")).thenReturn(Optional.of(ReducedNode.leaf("value", "leaf val")));
		var node = new SwitchNode(subNode);

		var joined = node.reduce("the name").orElseThrow();

		assertThat(joined.getName()).isEqualTo("the name");
		assertThat(joined.isLeaf()).isFalse();
		assertThat(joined.getChildren()).hasSize(2);

		var typeNode = joined.getSubNode("type");
		assertThat(typeNode)
				.isPresent();
		assertThat(typeNode.get().isLeaf())
				.isTrue();
		assertThat(typeNode.get().getValue())
				.as("type should reflect the name of the grammar that the node stems from")
				.isEqualTo("grammar-a");

		var valueNode = joined.getSubNode("value");
		assertThat(valueNode).isPresent();
		assertThat(valueNode.get().isLeaf())
				.as("the subnode returned a leaf")
				.isTrue();
		assertThat(valueNode.get().getValue())
				.isEqualTo("leaf val");
	}

	@Test
	void reduceWithTypeNode() {
		var subNode = mock(AstNode.class);
		when(subNode.getOriginGrammarName()).thenReturn("grammar-a");
		when(subNode.reduce("value")).thenReturn(Optional.of(ReducedNode.node("value", List.of())));
		var node = new SwitchNode(subNode);

		var joined = node.reduce("the name").orElseThrow();

		assertThat(joined.getName()).isEqualTo("the name");
		assertThat(joined.isLeaf()).isFalse();
		assertThat(joined.getChildren()).hasSize(2);

		var typeNode = joined.getSubNode("type");
		assertThat(typeNode)
				.isPresent();
		assertThat(typeNode.get().isLeaf())
				.isTrue();
		assertThat(typeNode.get().getValue())
				.as("type should reflect the name of the grammar that the node stems from")
				.isEqualTo("grammar-a");

		var valueNode = joined.getSubNode("value");
		assertThat(valueNode).isPresent();
		assertThat(valueNode.get().isLeaf())
				.as("the subnode returned a node")
				.isFalse();
		assertThat(valueNode.get().getChildNames())
				.isEmpty();
	}

	@Test
	void reduceWithoutType() {
		var subNode = mock(AstNode.class);
		when(subNode.getOriginGrammarName()).thenReturn(null);
		when(subNode.reduce("the name")).thenReturn(Optional.of(ReducedNode.leaf("fake", "hello")));
		var node = new SwitchNode(subNode);

		var joined = node.reduce("the name").orElseThrow();

		assertThat(joined.getName())
				.as("the node returned from the subnode should be used")
				.isEqualTo("fake");
		assertThat(joined.isLeaf()).isTrue();
		assertThat(joined.getValue()).isEqualTo("hello");
	}

	@Override
	Stream<AstNodeTest<AstNode>.AstNodeProbe> getProbes(String reduceName) {
		AstNode tokenMock = getTokenNode("variable_name","LETTERS","appContext");
		AstNode tokenMock2 = getTokenNode("variable_name","LETTERS","appContext2");
		AstNode listMock = getListNode("array_items");
		AstNode listMock2 = getListNode("array_items",tokenMock,tokenMock2);
		AstNode mockNode = getMockNode(
				"grammar_name",
				ReducedNode.leaf("value","someValue"),
				"def someValue()"
		);
		return Stream.of(
				new AstNodeProbe(
						new SwitchNode(tokenMock),
						ReducedNode.node(reduceName,List.of(
								ReducedNode.leaf("type","variable_name"),
								ReducedNode.leaf("value","appContext")
						)),
						"appContext"
				),
				new AstNodeProbe(
						new SwitchNode(listMock),
						ReducedNode.node(reduceName,List.of(
								ReducedNode.leaf("type","array_items"),
								ReducedNode.node("value",List.of())
						)),
						""
				),
				new AstNodeProbe(
						new SwitchNode(listMock2),
						ReducedNode.node(reduceName,List.of(
								ReducedNode.leaf("type","array_items"),
								ReducedNode.node("value",List.of(
										ReducedNode.leaf("0","appContext"),
										ReducedNode.leaf("1","appContext2")
								))
						)),
						"appContextappContext2"
				),
				new AstNodeProbe(
						new SwitchNode(mockNode),
						ReducedNode.node(reduceName,List.of(
								ReducedNode.leaf("type","grammar_name"),
								ReducedNode.leaf("value","someValue")
						)),
						"def someValue()"
				)
		);
	}



}