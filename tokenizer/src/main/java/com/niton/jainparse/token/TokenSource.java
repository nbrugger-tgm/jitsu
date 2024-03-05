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
public class TokenSource<T extends Enum<T> & Tokenable> extends AbstractList<AssignedToken<T>> {
    @NonNull
    private final Reader reader;
    @Getter(AccessLevel.PRIVATE)
    private final LinkedList<AssignedToken<T>> readTokens = new LinkedList<>();
    private final Tokenizer<T> tokenizer;
    @Setter(AccessLevel.PRIVATE)
    private String buffer = "";
    @Setter(AccessLevel.PRIVATE)
    private boolean ended = false;
    private int chunkSize = 50;

    public TokenSource(InputStream reader, Tokenizer<T> tokenizer) {
        this(new InputStreamReader(reader), tokenizer);
    }

    public TokenSource(InputStream reader, T[] tokenTypes) {
        this(new InputStreamReader(reader), new Tokenizer<>(tokenTypes));
    }

    public TokenSource(@NonNull Reader reader, Tokenizer<T> tokenizer) {
        this.reader = reader;
        this.tokenizer = tokenizer;
    }

    public TokenSource(@NonNull Reader reader, T[] tokenTypes) {
        this(reader, new Tokenizer<>(tokenTypes));
    }

    public void setChunkSize(int chunkSize) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Chunk size must be greater than 0");
        }
        this.chunkSize = chunkSize;
    }

    @Override
    public AssignedToken<T> get(int index) {
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
        List<AssignedToken<T>> newTokens = new ArrayList<>();
        while (newTokens.size() < 2 && !ended) {
            enlargeBuffer();
            var tokenResult = tokenizer.tokenize(buffer);
            if (!tokenResult.wasParsed()) throw new RuntimeException(
                    tokenResult.exception().getMostProminentDeepException().getMessage()
            );
            newTokens = tokenResult.unwrap();
            newTokens = newTokens.stream()
                    .filter(
                            e -> e.getType() != DefaultToken.EOF
                    )
                    .collect(Collectors.toList());
        }
        if (!ended) {
            //remove the last token because the last token might be incomplete if it is a multi-char token
            newTokens.remove(newTokens.size() - 1);
        }
        readTokens.addAll(newTokens);
        AssignedToken<T> lastParsed = newTokens.get(newTokens.size() - 1);
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
