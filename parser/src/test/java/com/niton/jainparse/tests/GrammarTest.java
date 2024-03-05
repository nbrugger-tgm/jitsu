package com.niton.jainparse.tests;

import com.niton.jainparse.grammar.api.Grammar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class GrammarTest {
	@Test
	void disallowDirectRecursion() {
		var grm = Grammar.build("test-grm");
		grm.setDirectRecursion(false);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			grm.grammar("test-grm").add("content");
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			grm.grammar("test-grm").anyExcept().add("content");
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			grm.grammar("test-grm").ignore().add("content");
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			grm.grammar("test-grm").optional().add("content");
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			grm.grammar("test-grm").repeat().add("content");
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			grm.grammars("test-grm").anyExcept().add("content");
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			grm.grammars("test-grm").add("content");
		});
	}
}
