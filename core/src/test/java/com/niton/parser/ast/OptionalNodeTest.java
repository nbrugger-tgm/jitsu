package com.niton.parser.ast;

import java.util.stream.Stream;

class OptionalNodeTest extends AstNodeTest<OptionalNode>{

	@Override
	Stream<AstNodeTest<OptionalNode>.AstNodeProbe> getProbes(String reduceName) {
		var valueNode = AstNodeMocker.getTokenNode("grammar","ALL", "123abc");
		var noValueNode = AstNodeMocker.getMockNode("grammar2",null,"");
		return Stream.of(
				new AstNodeProbe(
						new OptionalNode(valueNode),
						ReducedNode.leaf(reduceName,"123abc"),
						"123abc"
				),
				new AstNodeProbe(
						new OptionalNode(noValueNode),
						null,
						""
				),
				new AstNodeProbe(
						new OptionalNode(),
						null,
						""
				)
		);
	}
}