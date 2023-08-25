package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer;

import java.util.List;
import java.util.stream.Stream;

class IgnoredNodeTest extends AstNodeTest<OptionalNode> {

    @Override
    Stream<AstNodeTest<OptionalNode>.AstNodeProbe> getProbes(String reduceName) {
        var subed = new IgnoredNode();
        subed.setValue(new TokenNode(
                List.of(new Tokenizer.AssignedToken("1a2b3c", "name", 2)),
                AstNode.Location.of(0, 0, 0, 6)
        ));


        return Stream.of(
                new AstNodeProbe(new IgnoredNode(), null, ""),
                new AstNodeProbe(subed, null, "1a2b3c")
        );
    }
}