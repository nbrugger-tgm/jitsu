package com.niton.parser.exceptions;

import com.niton.parser.token.TokenStream;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        var lastException = Arrays.stream(lastExceptions).max(Comparator.comparingInt(ParsingException::getIndex)).orElseThrow();
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

    @Override
    public String getMessage() {
        return super.getMessage() + " [" + grammarName + ":" + line + ":" + column + "]";
    }
    public String getRawMessage(){
        return super.getMessage();
    }

    public String getFullExceptionTree() {
        if (causes.isEmpty())
            return getMessage();
        return getMessage() + ":\n" + causes.stream().map(ParsingException::getFullExceptionTree)
                .map(bloc -> bloc.replace("\n", "\n  "))
                .collect(Collectors.joining("\n"));
    }

    private Stream<ParsingException> getMostProminentDeepExceptionStream() {
        if (causes.isEmpty())
            return Stream.of(this);
        return causes.stream().flatMap(ParsingException::getMostProminentDeepExceptionStream).filter(e -> e.index == this.index);
    }

    public ParsingException getMostProminentDeepException() {
        ParsingException[] deepCauses = getMostProminentDeepExceptionStream().toArray(ParsingException[]::new);
        return new ParsingException(grammarName, getMessage() + ":\n\t" + Arrays.stream(deepCauses)
                .map(ParsingException::getMessage)
                .distinct()
                .collect(Collectors.joining(" or,\n\t"))
                , deepCauses);
    }
}

