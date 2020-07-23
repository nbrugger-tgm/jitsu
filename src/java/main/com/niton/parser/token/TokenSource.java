package com.niton.parser.token;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Tokenizer.AssignedToken;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.AbstractList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class TokenSource extends AbstractList<AssignedToken> {
    @Setter
    private int chunkSize = 50;
    @Getter
    private Reader reader;
    @Setter
    @Getter
    private Tokenizer tokenizer = new Tokenizer();
    private LinkedList<AssignedToken> readTokens = new LinkedList<>();
    @Getter
    private String buffer = new String();
    @Getter
    private int bufferConsumedMarker = 0;
    private boolean ended = false;

    public TokenSource(Reader reader) {
        super();
        this.reader = reader;
    }

    public TokenSource(InputStream reader) {
        this(new InputStreamReader(reader));
    }

    @Override
    public AssignedToken get(int index) {
        while (readTokens.size() == 0 || readTokens.size() <= index) {
            try {
                readNextToken();
            } catch (IOException | ParsingException e) {
                RuntimeException rex = new RuntimeException("The next token was not readable due to an Exception");
                rex.initCause(e);
                throw rex;
            }
        }
        AssignedToken token = readTokens.get(index);
        if (index + 1 == readTokens.size() && !ended)
            try {
                readNextToken();
            } catch (IOException | ParsingException e) {
                RuntimeException rex = new RuntimeException("The next token was not readable due to an Exception");
                rex.initCause(e);
                throw rex;
            }
        return token;
    }

    private void readNextToken() throws IOException, ParsingException {
        tokenizer.setIgnoreEOF(true);
        if (bufferConsumedMarker >= buffer.length()) {
            if (ended)
                throw new IndexOutOfBoundsException("The requested token is out of bound");
            fillBuffer();
        }
        List<AssignedToken> newTokens = null;
        while (true) {
            newTokens = tokenizer.tokenize(buffer.substring(bufferConsumedMarker));
            newTokens = newTokens.stream()
                    .filter(
                            e ->!e.token.getRegex().pattern().equals(DefaultToken.EOF.pattern)
                    )
                    .collect(Collectors.toList());
            if (newTokens.size() < 2) {
                if (ended)
                    break;
                else
                    enlargeBuffer();
            } else {
                break;
            }
        }
        if(!ended)
            newTokens.remove(newTokens.size() - 1);
        readTokens.addAll(newTokens);
        AssignedToken lastParsed = newTokens.get(newTokens.size() - 1);
        bufferConsumedMarker = bufferConsumedMarker + lastParsed.start + lastParsed.value.length();
    }

    private void enlargeBuffer() throws IOException {
        char[] chars = new char[chunkSize];
        int cnt;
        ended = ((cnt = reader.read(chars)) == -1 || cnt < chunkSize);
        buffer = buffer + new String(chars);
    }

    private void fillBuffer() throws IOException {
        char[] chars = new char[chunkSize];
        int cnt;
        ended = ((cnt = reader.read(chars)) == -1 || cnt < chunkSize);
        buffer = new String(chars);
        bufferConsumedMarker = 0;
    }

    @Override
    public int size() {
        if (ended)
            return readTokens.size();
        return readTokens.size() + 1;
    }

    public boolean isEnded() {
        return ended;
    }

    @Override
    public String toString() {
        return "TokenSource{FUCK YOU}";
    }
}
