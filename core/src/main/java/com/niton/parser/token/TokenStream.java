package com.niton.parser.token;

public interface TokenStream {
    Tokenizer.AssignedToken next();

    int index();

    void elevate();

    void commit();

    void rollback();

    int level();

    int size();

    String getPreviousTokens(int count);

    boolean hasNext();

    /**
     * @param startIndex inclusive will also be the starting index of the new stream
     * @param endIndex  exclusive
     * @return a new TokenStream with the given range. The new stream will be independent from the old one.
     */
    TokenStream subStream(int startIndex, int endIndex);

    int getLine();
    int getColumn();
}
