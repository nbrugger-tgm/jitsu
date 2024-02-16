package com.niton.jainparse.token;

import com.niton.jainparse.token.Tokenizer.AssignedToken;
import lombok.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A lazy loading token source
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = false)
public class TokenSource extends AbstractList<AssignedToken> {
    @NonNull
    private final Reader reader;
    @Getter(AccessLevel.PRIVATE)
    private final LinkedList<AssignedToken> readTokens = new LinkedList<>();

    @Setter(AccessLevel.PRIVATE)
    private String buffer = "";
    @Setter(AccessLevel.PRIVATE)
    private boolean ended = false;

    private int chunkSize = 50;
    private Tokenizer tokenizer = new Tokenizer();

    public TokenSource(InputStream reader) {
        this(new InputStreamReader(reader));
    }

    public TokenSource(@NonNull Reader reader) {
        this.reader = reader;
    }

    public void setChunkSize(int chunkSize) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Chunk size must be greater than 0");
        }
        this.chunkSize = chunkSize;
    }

    @Override
    public AssignedToken get(int index) {
        while (readTokens.size() <= index) {
            if (ended) {
                outOfBoundsException(index);
            }
            try {
                readNextToken();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return readTokens.get(index);
    }

    private void outOfBoundsException(int index) {
        throw new IndexOutOfBoundsException(String.format(
                "There is no token at index %d(size: %d)",
                index,
                readTokens.size()
        ));
    }

    private void readNextToken() throws IOException {
        tokenizer.setIgnoreEOF(true);
        if (ended) {
            throw new IllegalStateException("All tokens read, no more tokens available");
        }
        List<AssignedToken> newTokens = new ArrayList<>();
        while (newTokens.size() < 2 && !ended) {
            enlargeBuffer();
            var tokenResult = tokenizer.tokenize(buffer);
            if (!tokenResult.wasParsed()) throw new RuntimeException(
                    tokenResult.exception().getMostProminentDeepException().getMessage()
            );
            newTokens = tokenResult.unwrap();
            newTokens = newTokens.stream()
                    .filter(
                            e -> !e.getName().equals(DefaultToken.EOF.name())
                    )
                    .collect(Collectors.toList());
        }
        if (!ended) {
            newTokens.remove(newTokens.size() - 1);
        }
        readTokens.addAll(newTokens);
        AssignedToken lastParsed = newTokens.get(newTokens.size() - 1);
        buffer = buffer.substring(lastParsed.getEnd());
    }

    private void enlargeBuffer() throws IOException {
        char[] chars = new char[chunkSize];
        int cnt;
        ended = ((cnt = reader.read(chars)) == -1 || cnt < chunkSize);
        if (cnt != -1) {
            buffer = buffer + new String(chars, 0, cnt);
        }
    }

    @Override
    public int size() {
        if (ended) {
            return readTokens.size();
        }
        return readTokens.size() + 1;
    }

    @Override
    public String toString() {
        return String.format("TokenSource{%s}", reader);
    }
}
