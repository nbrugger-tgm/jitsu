package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class IgnoredNodeTest extends AstNodeTest<IgnoredNode> {

	@Override
	Stream<AstNodeTest<IgnoredNode>.AstNodeProbe> getProbes(String reduceName) {
		var subed = new IgnoredNode();
		subed.setIgnored(List.of(new Tokenizer.AssignedToken("1a2b3c", "name", 2)));
		subed.saveRAM = false;

		var subed2 = new IgnoredNode();
		subed2.setIgnored(List.of(new Tokenizer.AssignedToken("333221", "name", 2)));
		subed2.saveRAM = true;

		return Stream.of(
				new AstNodeProbe(new IgnoredNode(), null, ""),
				new AstNodeProbe(subed, null, "1a2b3c"),
				new AstNodeProbe(subed2, null, "")
		);
	}
}