package com.niton.jainparse.api;

import com.niton.jainparse.exceptions.ParsingException;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ParsingResult<T> {
    static <T> ParsingResult<T> ok(T node) {
        return new Parsed<>(node);
    }

    static<T> ParsingResult<T> error(ParsingException exception) {
        return new NotParsed<>(exception);
    }

    boolean wasParsed();

    T unwrap();

    ParsingException exception();

    default void ifParsedOrElse(
            Consumer<T> ifParsed,
            Consumer<ParsingException> ifNotParsed
    ) {
        if (wasParsed())
            ifParsed.accept(unwrap());
        else
            ifNotParsed.accept(exception());
    }

    default <R> ParsingResult<R> map(Function<T, R> mapper){
        if(!wasParsed())
            return ParsingResult.error(exception());
        return ParsingResult.ok(mapper.apply(unwrap()));
    }

    default T orElse(Function<ParsingException, T> mapper) {
        if(!wasParsed())
            return mapper.apply(exception());
        return unwrap();
    }

    class Parsed<T> implements ParsingResult<T> {
        private final T node;

        public Parsed(T node) {
            this.node = node;
        }

        @Override
        public boolean wasParsed() {
            return true;
        }

        @Override
        public T unwrap() {
            return node;
        }

        @Override
        public ParsingException exception() {
            throw new IllegalStateException("This result was parsed");
        }
    }

    class NotParsed<T> implements ParsingResult<T> {
        private final ParsingException exception;

        public NotParsed(ParsingException exception) {
            this.exception = exception;
        }

        @Override
        public boolean wasParsed() {
            return false;
        }

        @Override
        public T unwrap() {
            throw new IllegalStateException("This result was not parsed");
        }

        @Override
        public ParsingException exception() {
            return exception;
        }
    }
}
