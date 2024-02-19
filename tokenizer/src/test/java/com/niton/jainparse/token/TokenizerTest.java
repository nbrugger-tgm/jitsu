package com.niton.jainparse.token;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.niton.jainparse.token.DefaultToken.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class TokenizerTest {
    @Test
    void tokenizeEmpty() {
        assertThatCode(() -> Tokenizer.createDefault().tokenize("")).doesNotThrowAnyException();
    }

    @Test
    void onlyUndefined() {
        Tokenizer<DefaultToken> tokenizer = new Tokenizer<>();
        var res = tokenizer.tokenize("A1").unwrap();
        assertThat(res.get(0)).extracting(AssignedToken::getValue).isEqualTo("A1");
        assertThat(res.get(0)).extracting(AssignedToken::getType).isNull();
    }

    @Test
    void mixUndefined() {
        var tokenizer = new Tokenizer<>(DefaultToken.NUMBER);
        var res = tokenizer.tokenize("2A2").unwrap();
        assertThat(res).hasSize(3);
        assertThat(res.get(1)).extracting(AssignedToken::getValue).isEqualTo("A");
        assertThat(res.get(1)).extracting(AssignedToken::getType).isNull();
        assertThat(res.get(0)).extracting(AssignedToken::getType).isNotNull();
        assertThat(res.get(2)).extracting(AssignedToken::getType).isNotNull();
    }

    enum Overlapping implements Tokenable {
        ANYTHING(".+"),
        NUMBER("[0-9]+");

        final String s;

        Overlapping(String s) {
            this.s = s;
        }

        @Override
        public String pattern() {
            return s;
        }
    }
    @Test
    void overlapPrevention() {
        var tokenizer = new Tokenizer<>(Overlapping.values());
        assertThat(tokenizer.tokenize("GDGAGDGF5")).isInstanceOf(ParsingResult.NotParsed.class);
    }

    @Test
    void useCase() {
        var tokenizer = new Tokenizer<>(DefaultToken.values());
        tokenizer.setIgnoreEOF(true);
        var tokens = tokenizer.tokenize("I  am a string").unwrap();
        assertThat(tokens)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        new AssignedToken<>("I", LETTERS, 0),
                        new AssignedToken<>("  ", WHITESPACE, 1),
                        new AssignedToken<>("am", LETTERS, 3),
                        new AssignedToken<>(" ", WHITESPACE, 5),
                        new AssignedToken<>("a", LETTERS, 6),
                        new AssignedToken<>(" ", WHITESPACE, 7),
                        new AssignedToken<>("string", LETTERS, 8)
                );
    }
}