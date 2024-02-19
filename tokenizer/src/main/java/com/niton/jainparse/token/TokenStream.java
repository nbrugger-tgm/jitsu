package com.niton.jainparse.token;

import com.niton.jainparse.api.Location;

import java.util.List;

public interface TokenStream<T extends Enum<T> & Tokenable> {
    Tokenizer.AssignedToken<T> next();

    int index();

    void elevate();

    void commit();

    void rollback();

    int level();

    int size();

    /**
     * @return the next token without consuming it
     */
    Tokenizer.AssignedToken<T> peek();

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

    static <T extends Enum<T> & Tokenable> TokenStream<T> of(List<Tokenizer.AssignedToken<T>> tokens) {
        return new ListTokenStream<>(tokens);
    }
}
