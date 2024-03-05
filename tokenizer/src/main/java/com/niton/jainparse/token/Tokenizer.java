package com.niton.jainparse.token;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.exceptions.ParsingException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The tokenizer is the first step of parsing and devides the string into classified chunks ({@link
 * AssignedToken}s
 *
 * @author Nils
 * @version 2019-05-27
 */
public class Tokenizer<T extends Enum<T> & Tokenable> {
    private final T[] tokenTypes;
    @Setter
    @Getter
    private boolean ignoreEOF = false;

    /**
     * Creates a default tokenizer with the {@link DefaultToken default tokens}
     */
    public static Tokenizer<DefaultToken> createDefault() {
        return new Tokenizer<>(DefaultToken.values());
    }

    @SafeVarargs
    public Tokenizer(T... tokens) {
        this.tokenTypes = tokens;
    }

    public Tokenizer(List<T> tokens) {
        tokenTypes = tokens.toArray((T[]) new Enum[tokens.size()]);
    }

    /**
     * Tokenizes the string. Maps each part of the string to a token. If no token in the tokenizer
     * matches a certain part UNDEFINED is used
     *
     * @param content the string to map to tokens
     * @return a list of assigned tokens.
     * @
     */
    public ParsingResult<List<AssignedToken<T>>> tokenize(String content) {
        List<AssignedToken<T>> assignedTokens = parseTokens(content);
        var err = verifyNoOverlap(assignedTokens);
        if (err != null)
            return ParsingResult.error(err);
        fillGaps(content, assignedTokens);
        return ParsingResult.ok(assignedTokens);
    }

    private List<AssignedToken<T>> parseTokens(String content) {
        List<AssignedToken<T>> parsed = new LinkedList<>();
        for (T tokenType : this.tokenTypes) {
            if (ignoreEOF && tokenType.pattern().equals(DefaultToken.EOF.regex)) {
                continue;
            }
            Pattern p = tokenType.compile();
            Matcher m = p.matcher(content);
            while (m.find()) {
                AssignedToken<T> res = new AssignedToken<>();
                res.type = tokenType;
                res.value = m.group();
                res.start = m.start();
                parsed.add(res);
            }
        }
        parsed.sort(Comparator.comparingInt((AssignedToken<T> o) -> o.start));
        return parsed;
    }

    private @Nullable ParsingException verifyNoOverlap(List<AssignedToken<T>> tokens) {
        int last = 0;
        AssignedToken<T> lastToken = null;
        var line = 1;
        var col = 1;

        for (var assignedToken : tokens) {
            if (assignedToken.start > last) {
                last = assignedToken.start;
            } else if (last > assignedToken.start) {
                return overlapException(assignedToken, lastToken, line, col);
            }
            last += assignedToken.value.length();
            lastToken = assignedToken;
            if (assignedToken.value.contains("\n")) {
                line++;
                col = 1;
            } else {
                col += assignedToken.value.length();
            }
        }
        return null;
    }

    private void fillGaps(String content, List<AssignedToken<T>> tokens) {
        int last = 0;
        List<AssignedToken<T>> undefined = new ArrayList<>(tokens.size());
        for (var assignedToken : tokens) {
            if (assignedToken.start > last) {
                var undef = new AssignedToken<T>(
                        content.substring(last, assignedToken.start),
                        null
                );
                undef.start = last;
                undefined.add(undef);
                last += undef.value.length();
            }
            last += assignedToken.value.length();
        }
        if (last < content.length()) {
            undefined.add(new AssignedToken<T>(
                    content.substring(last),
                    null
            ));
        }
        tokens.addAll(undefined);
        tokens.sort(Comparator.comparingInt((AssignedToken<T> o) -> o.start));
    }

    private ParsingException overlapException(
            AssignedToken<T> assignedToken,
            AssignedToken<T> overlapedWith,
            int line,
            int col
    ) {
        return new ParsingException("[Tokenizer]", String.format(
                "Tokens overlapping: %s overlaps previous Token %s!",
                assignedToken,
                overlapedWith
        ), Location.of(line, col, line, col + assignedToken.value.length()));
    }

    /**
     * An assigned token defines which token matches the part of the string stored in {@link
     * AssignedToken#value}
     */
    @Data
    @AllArgsConstructor
    public static class AssignedToken<T extends Enum<T> & Tokenable> {
        private String value;
        /**
         * Can be null when the token is not covered by any token type
         */
        @Nullable
        private T type;
        private int start;

        public AssignedToken(String value, @Nullable T name) {
            this.value = value;
            this.type = name;
        }

        public AssignedToken() {
        }

        public int getEnd() {
            return start + value.length();
        }

        /**
         * returns the matched string and the name of the token
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return value + " (" + type + ")";
        }
    }
}
