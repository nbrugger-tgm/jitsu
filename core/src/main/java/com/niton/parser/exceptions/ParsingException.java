package com.niton.parser.exceptions;

import com.niton.parser.token.Location;
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
    private final static Comparator<Location> locationComparator = Comparator.comparingInt(Location::getFromLine)
            .thenComparingInt(Location::getFromColumn)
            .thenComparingInt(Location::getToLine)
            .thenComparingInt(Location::getToColumn);
    private final String grammarName;
    private final Location location;
    private final List<ParsingException> causes;

    /**
     * Creates an Instance of ParsingException.java
     *
     * @param grammarName
     * @param message
     * @author Nils
     * @version 2019-05-29
     */
    public ParsingException(String grammarName, String message, Location location) {
        super(message);
        this.grammarName = grammarName;
        this.location = location;
        causes = List.of();
    }

    public ParsingException(String grammarName, String message, ParsingException lastException) {
        super(message, lastException);
        this.grammarName = grammarName;
        this.location = lastException.location;
        causes = List.of(lastException);
    }

    public ParsingException(String grammarName, String message, ParsingException[] lastExceptions) {
        super(message);
        this.grammarName = grammarName;
        var lastException = Arrays.stream(lastExceptions)
                .max((a, b) -> locationComparator.compare(a.location, b.location))
                .orElseThrow();
        this.location = lastException.location;
        causes = List.of(lastExceptions);
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " [" + grammarName + ": " + location.format() + "]";
    }

    public String getRawMessage() {
        return super.getMessage();
    }

    public String markInText(String code, int context) {
        return location.markInText(code, context, getMessage());
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
        return causes.stream().flatMap(ParsingException::getMostProminentDeepExceptionStream)
                .filter(this::hasSameStart);
    }

    private boolean hasSameStart(ParsingException e) {
        return e.location.getFromLine() == this.location.getFromLine() && e.location.getFromColumn() == this.location.getFromColumn();
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

