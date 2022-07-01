package com.niton.parser.ast;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ListNodeTest extends AstNodeTest<ListNode> {

	@Override
	Stream<AstNodeTest<ListNode>.AstNodeProbe> getProbes(String reduceName) {
		var tok = AstNodeMocker.getTokenNode("grammar","LETTERS","somVal");
		var tok1 = AstNodeMocker.getTokenNode("grammar1","NUMBERS","222");
		var tok2 = AstNodeMocker.getTokenNode("grammar2","NUMBERS","333");
		return Stream.of(
				new AstNodeProbe(
						new ListNode(List.of()),
						ReducedNode.node(reduceName, List.of()),
						""
				),
				new AstNodeProbe(
						new ListNode(List.of(tok)),
						ReducedNode.node(reduceName, List.of(
								ReducedNode.leaf("0","somVal")
						)),
						"somVal"
				),
				new AstNodeProbe(
						new ListNode(List.of(tok1,tok2)),
						ReducedNode.node(reduceName, List.of(
								ReducedNode.leaf("0","222"),
								ReducedNode.leaf("1","333")
						)),
						"222333"
				)
		);
	}
}