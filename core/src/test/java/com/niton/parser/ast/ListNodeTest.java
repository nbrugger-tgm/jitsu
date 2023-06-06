package com.niton.parser.ast;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

class ListNodeTest extends AstNodeTest<SequenceNode> {

	@Override
	Stream<AstNodeTest<SequenceNode>.AstNodeProbe> getProbes(String reduceName) {
		var tok = AstNodeMocker.getTokenNode("grammar","LETTERS","somVal");
		var tok1 = AstNodeMocker.getTokenNode("grammar1","NUMBERS","222");
		var tok2 = AstNodeMocker.getTokenNode("grammar2","NUMBERS","333");
		return Stream.of(
				new AstNodeProbe(
						new SequenceNode(explicitLocation),
						ReducedNode.node(reduceName, List.of()),
						""
				),
				new AstNodeProbe(
						new SequenceNode(List.of(tok), Map.of("0",0)),
						ReducedNode.node(reduceName, List.of(
								ReducedNode.leaf("0","somVal")
						)),
						"somVal"
				),
				new AstNodeProbe(
						new SequenceNode(List.of(tok1,tok2), Map.of("0",0,"1",1)),
						ReducedNode.node(reduceName, List.of(
								ReducedNode.leaf("0","222"),
								ReducedNode.leaf("1","333")
						)),
						"222333"
				)
		);
	}
}