package com.niton.parser.token;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Tokenizer.AssignedToken;
import org.assertj.core.api.SoftAssertionsProvider.ThrowingRunnable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.*;

class TokenStreamTest {


	@Test
	void nextReturnsCorrect() {
		var         token  = new AssignedToken();
		TokenStream stream = new TokenStream(List.of(token));
		assertThat(stream.next()).isEqualTo(token);
	}

	@Test
	void nextOverflowException1() {
		var         token  = new AssignedToken();
		TokenStream stream = new TokenStream(List.of(token));
		stream.next();

		assertThatCode(stream::next).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void nextOverflowException2() {
		TokenStream stream = new TokenStream(List.of());

		assertThatCode(stream::next).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void nextOrder() {
		var         token1 = new AssignedToken("1", "test");
		var         token2 = new AssignedToken("2", "test");
		var         token3 = new AssignedToken("3", "test");
		TokenStream stream = new TokenStream(List.of(token1, token2, token3));

		assertThat(stream.next()).isEqualTo(token1);
		assertThat(stream.next()).isEqualTo(token2);
		assertThat(stream.next()).isEqualTo(token3);
	}

	@Test
	void nextShouldIncreaseIndex() {
		var         token1 = new AssignedToken("1", "test");
		var         token2 = new AssignedToken("2", "test");
		var         token3 = new AssignedToken("3", "test");
		var         token4 = new AssignedToken("4", "test");
		var         token5 = new AssignedToken("5", "test");
		TokenStream stream = new TokenStream(List.of(token1, token2, token3, token4, token5));
		assertThat(stream.index()).isZero();
		stream.next();
		assertThat(stream.index()).isEqualTo(1);
		stream.next();
		assertThat(stream.index()).isEqualTo(2);
	}


	@Test
	void increaseAffectsIndex() {
		var token1 = new AssignedToken("1", "test");
		var stream = new TokenStream(List.of(token1));
		assertThat(stream.index()).isZero();
		stream.increase();
		assertThat(stream.index()).isEqualTo(1);
		stream.increase();
		assertThat(stream.index()).isEqualTo(2);
	}

	@Test
	void increaseNoCheck() {
		var stream = new TokenStream(List.of());
		assertThat(stream.index()).isZero();
		for (int i = 0; i < 10; i++) {
			stream.increase();
		}
		assertThat(stream.index()).isEqualTo(10);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 5, 20})
	void elevateElevatesIndex(int streamIndex) throws ParsingException {
		var stream = new TokenStream(List.of());
		stream.index(streamIndex);
		assertThat(stream.index()).isEqualTo(streamIndex);
		for (int i = 0; i < 10; i++) {
			stream.elevate();
		}
		assertThat(stream.index()).isEqualTo(streamIndex);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 5, 20})
	void elevateElevatesLevel(int streamIndex) throws ParsingException {
		var stream = new TokenStream(List.of());
		assertThat(stream.level()).isZero();
		for (int i = 0; i < streamIndex; i++) {
			stream.elevate();
		}
		assertThat(stream.level()).isEqualTo(streamIndex);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 5, 7})
	void commitFailOnLowestLevel(int deep) {
		failOnLowestLevel(deep, TokenStream::commit);
	}

	private void failOnLowestLevel(int deep, Consumer<TokenStream> consumer) {
		var stream = new TokenStream(List.of());
		range(0, deep).forEach(i -> silence(stream::elevate));
		assertThatCode(() -> range(0, deep).forEach(i -> consumer.accept(stream)))
				.doesNotThrowAnyException();
		assertThatCode(() -> consumer.accept(stream)).isInstanceOf(IndexOutOfBoundsException.class);
	}

	private void silence(ThrowingRunnable elevate) {
		try {
			elevate.run();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void commitOverwritesPreviousLevelIndex() {
		var stream = new TokenStream(List.of());
		stream.index(5);
		stream.elevate();
		stream.index(10);
		stream.elevate();
		stream.index(20);
		stream.elevate();

		assertThat(stream.index()).isEqualTo(20);
		stream.index(30);
		stream.commit();
		assertThat(stream.index()).isEqualTo(30);
		stream.commit();
		assertThat(stream.index()).isEqualTo(30);
		stream.index(0);
		stream.commit();
		assertThat(stream.index()).isZero();
	}

	@Test
	void commitReducesLevel() {
		shouldReduceLevel(TokenStream::commit);
	}

	void shouldReduceLevel(Consumer<TokenStream> action) {

		var stream = new TokenStream(List.of());
		stream.elevate();
		stream.elevate();
		stream.elevate();

		assertThat(stream.level()).isEqualTo(3);
		action.accept(stream);
		assertThat(stream.level()).isEqualTo(2);
		action.accept(stream);
		assertThat(stream.level()).isEqualTo(1);
		action.accept(stream);
		assertThat(stream.level()).isZero();
	}

	@Test
	void rollbackResetsIndex() {
		var stream = new TokenStream(List.of());
		stream.index(5);
		stream.elevate();
		stream.index(10);
		stream.elevate();
		stream.index(20);
		stream.elevate();
		stream.index(30);

		assertThat(stream.index()).isEqualTo(30);
		stream.rollback();
		assertThat(stream.index()).isEqualTo(20);
		stream.index(0);
		stream.rollback();
		assertThat(stream.index()).isEqualTo(10);
		stream.rollback();
		assertThat(stream.index()).isEqualTo(5);
	}

	@Test
	void rollbackReducesLevel() {
		shouldReduceLevel(TokenStream::rollback);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 5, 7})
	void rollbackFailOnLowestLevel(int deep) {
		failOnLowestLevel(deep, TokenStream::rollback);
	}

	@Test
	void getReturnsCorrectIndex() {
		var stream = new TokenStream(List.of(
				new AssignedToken("1", "test"),
				new AssignedToken("2", "test")
		));
		assertThat(stream.get(0))
				.usingRecursiveComparison()
				.isEqualTo(new AssignedToken(
						"1",
						"test"
				));
		assertThat(stream.get(1))
				.usingRecursiveComparison()
				.isEqualTo(new AssignedToken(
						"2",
						"test"
				));
	}

	@Test
	void getDoesNotChangeLevel() {
		var stream = new TokenStream(List.of(
				new AssignedToken("1", "test"),
				new AssignedToken("2", "test")
		));
		assertThat(stream.level()).isZero();
		stream.get(1);
		assertThat(stream.level()).isZero();
	}

	@Test
	void getDoesNotChangeIndex() {
		var stream = new TokenStream(List.of(
				new AssignedToken("1", "test"),
				new AssignedToken("2", "test")
		));
		assertThat(stream.index()).isZero();
		stream.get(1);
		assertThat(stream.index()).isZero();
	}

	@Test
	void getFailsOutOfBounds() {
		var stream = new TokenStream(List.of(
				new AssignedToken("1", "test"),
				new AssignedToken("2", "test")
		));
		assertThatCode(() -> stream.get(2)).isInstanceOf(IndexOutOfBoundsException.class);
		assertThatCode(() -> stream.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	void size() {
		var stream = new TokenStream(List.of(
				new AssignedToken("1", "test"),
				new AssignedToken("2", "test")
		));
		assertThat(stream.size()).isEqualTo(2);

		assertThat(new TokenStream(List.of()).size()).isZero();
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 5, 7})
	void setRecursionLevelLimit() {
		var stream = new TokenStream(List.of());
		stream.setRecursionLevelLimit(5);
		assertThat(stream.getRecursionLevelLimit()).isEqualTo(5);
		assertThatCode(() -> {
			for (int i = 0; i < 6; i++) {
				stream.elevate();
			}
		}).isInstanceOf(IllegalStateException.class);
	}
}