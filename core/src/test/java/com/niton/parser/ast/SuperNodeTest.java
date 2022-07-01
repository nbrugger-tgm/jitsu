package com.niton.parser.ast;

import java.util.List;
import java.util.stream.Stream;

import static com.niton.parser.ast.ReducedNode.*;
import static org.junit.jupiter.api.Assertions.*;

class SuperNodeTest extends AstNodeTest<SuperNode> {

	@Override
	Stream<AstNodeTest<SuperNode>.AstNodeProbe> getProbes(String reduceName) {
		var empty       = new SuperNode();
		var onlyUnnamed = new SuperNode();
		onlyUnnamed.add(AstNodeMocker.getTokenNode("numbas", "NAME", "123"));
		onlyUnnamed.add(AstNodeMocker.getMockNode("ignored", null, ""));
		onlyUnnamed.add(AstNodeMocker.getTokenNode("numbas", "NAME", "456"));
		var onlyEmpty = new SuperNode();
		onlyEmpty.add(AstNodeMocker.getMockNode("ignored", null, ""));
		onlyEmpty.add(AstNodeMocker.getMockNode("ignored2", null, ""));
		var nested = new SuperNode();
		nested.add(AstNodeMocker.getMockNode("ignored", null, ""));
		nested.add(AstNodeMocker.getListNode(
				"ignored2",
				AstNodeMocker.getTokenNode("gram1", "tok1", "abc"),
				AstNodeMocker.getTokenNode("gram1", "tok1", "def")
		));
		var nestedNamed = new SuperNode();
		nestedNamed.name("ignored", AstNodeMocker.getTokenNode("ignored", "yeet", "yeet"));
		nestedNamed.add(AstNodeMocker.getListNode(
				"ignored2",
				AstNodeMocker.getTokenNode("gram1", "tok1", "abc"),
				AstNodeMocker.getTokenNode("gram1", "tok1", "def")
		));
		var normal = new SuperNode();
		normal.name("ignored", AstNodeMocker.getTokenNode("ignored", "yeet", "yeet2"));
		normal.name("list", AstNodeMocker.getListNode(
				"ignored2",
				AstNodeMocker.getTokenNode("gram1", "tok1", "abc"),
				AstNodeMocker.getTokenNode("gram1", "tok1", "def")
		));
		return Stream.of(
				new AstNodeProbe(empty, node(reduceName, List.of(leaf("value", ""))), ""),
				new AstNodeProbe(
						onlyUnnamed,
						node(reduceName, List.of(leaf("value", "123456"))),
						"123456"
				),
				new AstNodeProbe(onlyEmpty, node(reduceName, List.of(leaf("value", ""))), ""),
				new AstNodeProbe(nested, node(reduceName, List.of(
						leaf("0", "abc"),
						leaf("1", "def")
				)), "abcdef"),
				new AstNodeProbe(nestedNamed, node(reduceName, List.of(
						leaf("ignored", "yeet")
				)), "yeetabcdef"),
				new AstNodeProbe(normal, node(reduceName, List.of(
						leaf("ignored", "yeet2"),
						node("list", List.of(
								leaf("0", "abc"),
								leaf("1", "def")
						))
				)), "yeet2abcdef")
		);
	}
}