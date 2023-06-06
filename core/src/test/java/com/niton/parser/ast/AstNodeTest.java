package com.niton.parser.ast;

import lombok.Data;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.util.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

abstract class AstNodeTest<T extends AstNode> {
	@Data
	class AstNodeProbe {
		private final T           node;
		private final ReducedNode expectedReducedResult;
		private final String      joinedString;
	}

	/**
	 * @param reduceName the name used to create the reduced node
	 *
	 * @return a list of testcases
	 */
	abstract Stream<AstNodeProbe> getProbes(String reduceName);

	@TestFactory
	Stream<DynamicTest> reduceNullName() {
		return getProbes("coffee").map(probe -> DynamicTest.dynamicTest(
				probe.toString(),
				() -> assertThatThrownBy(() -> probe.node.reduce(null))
						.as("reduce(null) should throw NullPointerException")
						.isInstanceOf(NullPointerException.class)
		));
	}

	@TestFactory
	Stream<DynamicTest> reduce() {
		return getProbes("my special name").map(probe -> DynamicTest.dynamicTest(
				Objects.toString(probe.expectedReducedResult),
				() -> assertThat(probe.node.reduce("my special name"))
						.isEqualTo(Optional.ofNullable(probe.expectedReducedResult))
		));
	}

	@TestFactory
	Stream<DynamicTest> joinTokens() {
		return getProbes("java").map(probe -> DynamicTest.dynamicTest(
				StringUtils.isNotBlank(probe.joinedString) ? probe.joinedString : "(empty)",
				() -> assertThat(probe.node.joinTokens())
						.isEqualTo(probe.joinedString)
		));
	}
}