package com.niton.jainparse.token;

import com.niton.jainparse.api.Location;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface TokenStream<T extends Enum<T> & Tokenable> {
    default Tokenizer.AssignedToken<T> next() {
        return nextOptional().orElseThrow();
    }

    /**
     * Retrieves the next token from the token stream wrapped in an Optional object and moves the stream forward.
     *
     * @return an Optional containing the next token, or an empty Optional if there are no more tokens in the stream.
     */
    Optional<Tokenizer.AssignedToken<T>> nextOptional();

    int index();

    void elevate();

    void commit();

    void rollback();

    int level();

    int size();

    /**
     * @return the next token without consuming it
     */

    default Tokenizer.AssignedToken<T> peek() {
        return peekOptional().orElseThrow();
    }

    /**
     * Retrieves the next token from the token stream without consuming it.
     *
     * @return an Optional containing the next token, or an empty Optional if there are no more tokens in the stream.
     * @see #peek()
     */
    Optional<Tokenizer.AssignedToken<T>> peekOptional();


    String getPreviousTokens(int count);

    boolean hasNext();

    /**
     * @param startIndex inclusive will also be the starting index of the new stream
     * @param endIndex  exclusive
     * @return a new TokenStream with the given range. The new stream will be independent from the old one.
     */
    TokenStream<T> subStream(int startIndex, int endIndex);

    int getLine();
    int getColumn();

    Location currentLocation();
    Location lastConsumedLocation();

    static <T extends Enum<T> & Tokenable> TokenStream<T> of(List<Tokenizer.AssignedToken<T>> tokens) {
        return new ListTokenStream<>(tokens);
    }
}
