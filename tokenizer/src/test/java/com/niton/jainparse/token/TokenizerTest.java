package com.niton.jainparse.token;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.exceptions.ParsingException;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.niton.jainparse.token.DefaultToken.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class TokenizerTest {
    @Test
    void tokenizeEmpty() {
        assertThatCode(() -> new Tokenizer().tokenize("")).doesNotThrowAnyException();
    }

    @Test
    void onlyUndefined() {
        Tokenizer tokenizer = new Tokenizer(List.of());
        var res = tokenizer.tokenize("A1").unwrap();
        assertThat(res.get(0)).extracting(AssignedToken::getValue).isEqualTo("A1");
        assertThat(res.get(0)).extracting(AssignedToken::getName).isEqualTo("UNDEFINED");
    }

    @Test
    void mixUndefined() {
        Tokenizer tokenizer = new Tokenizer(DefaultToken.NUMBER);
        var res = tokenizer.tokenize("2A2").unwrap();
        assertThat(res).hasSize(3);
        assertThat(res.get(1)).extracting(AssignedToken::getValue).isEqualTo("A");
        assertThat(res.get(1)).extracting(AssignedToken::getName).isEqualTo("UNDEFINED");
        assertThat(res.get(0)).extracting(AssignedToken::getName).isNotEqualTo("UNDEFINED");
        assertThat(res.get(2)).extracting(AssignedToken::getName).isNotEqualTo("UNDEFINED");
    }

    @Test
    void overlapPrevention() {
        Tokenizer tokenizer = new Tokenizer(List.of(DefaultToken.NUMBER, new Tokenable() {
            @Override
            public String name() {
                return "ANYTHING";
            }

            @Override
            public String pattern() {
                return ".+";
            }
        }));
        assertThat(tokenizer.tokenize("GDGAGDGF5")).isInstanceOf(ParsingResult.NotParsed.class);
    }

    @Test
    void useCase() {
        Tokenizer tokenizer = new Tokenizer();
        tokenizer.setIgnoreEOF(true);
        var tokens = tokenizer.tokenize("I  am a string").unwrap();
        assertThat(tokens)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        new AssignedToken("I", LETTERS.name(), 0),
                        new AssignedToken("  ", WHITESPACE.name(), 1),
                        new AssignedToken("am", LETTERS.name(), 3),
                        new AssignedToken(" ", WHITESPACE.name(), 5),
                        new AssignedToken("a", LETTERS.name(), 6),
                        new AssignedToken(" ", WHITESPACE.name(), 7),
                        new AssignedToken("string", LETTERS.name(), 8)
                );
    }
}