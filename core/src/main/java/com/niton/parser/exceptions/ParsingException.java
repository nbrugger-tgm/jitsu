package com.niton.parser.exceptions;

import com.niton.parser.token.TokenStream;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An exception that happens during parsing
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
public class ParsingException extends Exception {
    private final String grammarName;
    private final int line;
    private final int column;
    private final int index;
    private final List<ParsingException> causes;

    /**
     * Creates an Instance of ParsingException.java
     *
     * @param grammarName
     * @param message
     * @author Nils
     * @version 2019-05-29
     */
    public ParsingException(String grammarName, String message, TokenStream stream) {
        super(message);
        this.grammarName = grammarName;
        line = stream.getLine();
        column = stream.getColumn();
        index = stream.index();
        causes = List.of();
    }

    public ParsingException(String grammarName, String message, ParsingException lastException) {
        super(message, lastException);
        this.grammarName = grammarName;
        this.line = lastException.line;
        this.column = lastException.column;
        this.index = lastException.index;
        causes = List.of(lastException);
    }

    public ParsingException(String grammarName, String message, ParsingException[] lastExceptions) {
        super(message);
        this.grammarName = grammarName;
        var lastException = getMostProminentException(lastExceptions);
        this.line = lastException.line;
        this.column = lastException.column;
        this.index = lastException.index;
        causes = List.of(lastExceptions);
    }

    public ParsingException(String grammarName, String format, int line, int col, int start) {
        super(format);
        this.grammarName = grammarName;
        this.line = line;
        this.column = col;
        this.index = start;
        causes = List.of();
    }

    private ParsingException getMostProminentException(ParsingException[] lastExceptions) {
        return Arrays.stream(lastExceptions)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(ParsingException::getIndex))
                .orElseThrow(() -> new IllegalArgumentException("No exceptions given"));
    }

    public ParsingException getMostProminentException() {
        if (causes.size() == 0)
            return this;
        return getMostProminentException(causes.toArray(new ParsingException[0]));
    }

    public ParsingException getMostProminentDeepException() {
        var mostProminent = getMostProminentException();
        if (mostProminent.getCauses().size() == 0)
            return mostProminent;
        return mostProminent.getMostProminentDeepException();
    }

    @Override
    public String getMessage() {
        return super.getMessage()+" ["+grammarName+":"+line+":"+column+"]";
    }

    public String getFullExceptionTree() {
        if (causes.size() == 0)
            return getMessage();
        return getMessage() + ":\n" + causes.stream().map(ParsingException::getFullExceptionTree)
                .map(bloc -> {
            return bloc.replace("\n", "\n\t");
        }).collect(Collectors.joining("\n"));
    }
}

