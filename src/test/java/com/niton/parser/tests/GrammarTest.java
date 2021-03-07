package com.niton.parser.tests;

import com.niton.parser.grammars.ChainGrammar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class GrammarTest {
	@Test
	public void disallowDirectRecursion() {
		ChainGrammar grm = new ChainGrammar();
		grm.setDirectRecursion(false);
		grm.setName("test-grm");
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
