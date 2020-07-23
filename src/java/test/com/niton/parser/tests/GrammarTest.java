package com.niton.parser.tests;

import com.niton.parser.grammars.ChainGrammar;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class GrammarTest {
    @Test
    public void disallowDirectRecursion(){
        ChainGrammar grm = new ChainGrammar();
        grm.setDirectRecursion(false);
        grm.setName("test-grm");
        Assertions.assertThrows(IllegalArgumentException.class,() -> {
            grm.grammar("test-grm").match().name("content");
        });

        Assertions.assertThrows(IllegalArgumentException.class,() -> {
            grm.grammar("test-grm").anyExcept().name("content");
        });

        Assertions.assertThrows(IllegalArgumentException.class,() -> {
            grm.grammar("test-grm").ignore().name("content");
        });

        Assertions.assertThrows(IllegalArgumentException.class,() -> {
            grm.grammar("test-grm").optional().name("content");
        });

        Assertions.assertThrows(IllegalArgumentException.class,() -> {
            grm.grammar("test-grm").repeat().name("content");
        });

        Assertions.assertThrows(IllegalArgumentException.class,() -> {
            grm.grammars("test-grm").anyExcept().name("content");
        });

        Assertions.assertThrows(IllegalArgumentException.class,() -> {
            grm.grammars("test-grm").match().name("content");
        });
    }
}
