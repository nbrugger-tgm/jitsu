package com.niton.jainparse.token;

import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TokenSourceTest {
    @Nested
    @DisplayName("Functionality")
    class FunctionalChunkTests {
        @Test
        void smallChunkTest() {
            StringReader reader = new StringReader("...WORT NOCHEINWORT98764ZAHL");
            var source = new TokenSource<>(reader, DefaultToken.values());
            assertEquals("", source.getBuffer());
            source.setChunkSize(1);
            assertEquals("", source.getBuffer());
            source.get(7);
            assertThat(source).hasSize(8);
            assertEquals(source.get(5).getValue(), "NOCHEINWORT");
        }

        @Test
        void avgChunkTest() {
            StringReader reader = new StringReader("...WORT WORT98764ZAHL");
            var source = new TokenSource<>(reader, DefaultToken.values());
            assertEquals("", source.getBuffer());
            source.setChunkSize(8);
            assertEquals("", source.getBuffer());
            source.get(7);
            assertThat(source).hasSize(8);
            assertEquals(source.get(5).getValue(), "WORT");
        }

        @Test
        void bigChunkTest() {
            StringReader reader = new StringReader("...WORT WORT98764ZAHL");
            var source = new TokenSource<>(reader, DefaultToken.values());
            assertEquals("", source.getBuffer());
            source.get(7);
            assertThat(source).hasSize(8);
            assertEquals(source.get(5).getValue(), "WORT");
        }
    }

    @Nested
    @DisplayName("Behaviour")
    class BehaviourTest {
        @Test
        void noInitialBuffer() throws IOException {
            var reader = mock(Reader.class);
            var source = new TokenSource<>(reader, DefaultToken.values());
            assertThat(source.getBuffer()).isEmpty();
            verify(reader, never()).read((char[]) any());
            verify(reader, never()).read(any(), anyInt(), anyInt());
            verify(reader, never()).read();
        }

        @Test
        void remainingBuffer() {
            var source = new TokenSource<>(new StringReader("ABCDEFGHIJKLMNOPQRSTUVWXYZ"), DefaultToken.values());
            source.setChunkSize(1);
            source.get(0);
            assertThat(source.getBuffer()).isEmpty();
        }

        @Test
        void remainingBuffer2() {
            var source = new TokenSource<>(new StringReader("ABCD EFGHIJKLMNOPQRSTUVWXYZ"), DefaultToken.values());
            source.setChunkSize(1);
            source.get(0);
            assertThat(source.getBuffer()).isEqualTo(" ");
        }

        @Test
        void remainingBuffer3() {
            var source = new TokenSource<>(new StringReader("ABCD EF"), DefaultToken.values());
            source.setChunkSize(3);
            source.get(0);
            assertThat(source.getBuffer()).isEqualTo("E");
        }

        @TestFactory
        Stream<DynamicTest> remaningBufferGeneric() {
            return IntStream.of(2, 4, 6).boxed().flatMap(
                    sz -> Stream.of(
                            "ABCDREF890723891",
                            "ANDADADAJHFKA ",
                            " DASDHJAGSDH",
                            "AAAAAAAAAAA       AAAAAAA"
                    ).map(input -> DynamicTest.dynamicTest(
                            format(
                                    "remainingBufferGeneric(%d, %s)",
                                    sz,
                                    input
                            ),
                            () -> {
                                var source = new TokenSource<>(new StringReader(input), DefaultToken.values());
                                source.setChunkSize(sz);
                                var res = source.get(0);
                                assertThat(res.getStart()).isZero();
                                assertThat(source.getBuffer()).doesNotStartWith(
                                        res.getValue());
                            }
                    ))
            );
        }

        @Nested
        class MethodsTest {
            @Test
            void getOutOfBounds() {
                var source = new TokenSource<>(new StringReader("ABC"), DefaultToken.values());
                assertThatCode(() -> source.get(1)).isInstanceOf(IndexOutOfBoundsException.class);
                assertThatCode(() -> source.get(3)).isInstanceOf(IndexOutOfBoundsException.class);
                assertThatCode(() -> source.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
            }

            @Test
            void getOrder() {
                var source = new TokenSource<>(new StringReader("ABC123+"), DefaultToken.values());
                assertThat(source.get(0))
                        .isNotNull()
                        .extracting(AssignedToken::getValue)
                        .isEqualTo("ABC");
                assertThat(source.get(1))
                        .isNotNull()
                        .extracting(AssignedToken::getValue)
                        .isEqualTo("123");
                assertThat(source.get(2))
                        .isNotNull()
                        .extracting(AssignedToken::getValue)
                        .isEqualTo("+");
            }

            @Test
            void sizeInitial0() {
                var source = new TokenSource<>(new StringReader("ABC123+"), DefaultToken.values());
                assertThat(source).hasSize(1);
            }

            @Test
            void sizeAfterFetch() {
                var source = new TokenSource<>(new StringReader("ABC123+"), DefaultToken.values());
                source.setChunkSize(1);
                source.get(0);
                assertThat(source).hasSize(2);
            }

            @Test
            void sizeAfterEof() {
                var source = new TokenSource<>(new StringReader("ABC123+"), DefaultToken.values());
                source.setChunkSize(1);
                source.get(2);
                assertThat(source).hasSize(3);
            }

            @Test
            void isEndedOnInit() {
                assertThat(new TokenSource<>(new StringReader(""), DefaultToken.values()).isEnded()).isFalse();
                assertThat(new TokenSource<>(new StringReader("ABC123+"), DefaultToken.values()).isEnded()).isFalse();
            }

            @Test
            void isEndedAfterReadingAllSmallChunk() throws IOException {
                var reader = new StringReader("ABC123+");
                var source = new TokenSource<>(reader, DefaultToken.values());
                source.setChunkSize(1);
                source.get(0);
                source.get(1);
                source.get(2);
                assertThat(source.isEnded()).isTrue();
                assertThat(reader.read()).isEqualTo(-1);
            }

            @Test
            void isEndedAfterReadingAllBigChunk() throws IOException {
                var reader = new StringReader("ABC123+");
                var source = new TokenSource<>(reader, DefaultToken.values());
                source.setChunkSize(8);
                source.get(0);
                assertThat(source.isEnded()).isTrue();
                assertThat(reader.read()).isEqualTo(-1);
            }

            @Test
            void isEndedAfterPartialSmallChunk() throws IOException {
                var reader = new StringReader("ABC123+");
                var source = new TokenSource<>(reader, DefaultToken.values());
                source.setChunkSize(1);
                source.get(0);
                assertThat(source.isEnded()).isFalse();
                assertThat(reader.read()).isNotEqualTo(-1);
            }

            @Test
            void setTokenizer() {
                Tokenizer<DefaultToken> tokenizer = mock();
                when(tokenizer.tokenize("ABC123+")).thenReturn(ParsingResult.ok(List.of(
                        new AssignedToken<>("ABC123+", DefaultToken.DOLLAR)
                )));
                var source = new TokenSource<>(new StringReader("ABC123+"),tokenizer);
                assertThat(source.getTokenizer()).isSameAs(tokenizer);
                assertThat(source.get(0)).isNotNull()
                        .extracting(AssignedToken::getValue)
                        .isEqualTo("ABC123+");
                verify(tokenizer).tokenize("ABC123+");
            }
        }
    }
}
