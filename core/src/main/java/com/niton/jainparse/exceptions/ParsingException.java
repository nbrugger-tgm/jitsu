package com.niton.jainparse.exceptions;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.internal.Lazy;
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
public class ParsingException {
    private final static Comparator<Location> locationComparator = Comparator.comparingInt(Location::getFromLine)
            .thenComparingInt(Location::getFromColumn)
            .thenComparingInt(Location::getToLine)
            .thenComparingInt(Location::getToColumn);
    private final String grammarName;
    private final Lazy<Location> location;
    private final List<ParsingException> causes;
    private final String message;

    /**
     * Creates an Instance of ParsingException.java
     *
     * @param grammarName
     * @param message
     * @author Nils
     * @version 2019-05-29
     */
    public ParsingException(String grammarName, String message, Location location) {
        this.message = message;
        this.grammarName = grammarName;
        this.location = new Lazy<>(location);
        causes = List.of();
    }

    public ParsingException(String grammarName, String message, ParsingException lastException) {
        this.message = message;
        ;
        this.grammarName = grammarName;
        this.location = lastException.location;
        causes = List.of(lastException);
    }

    public ParsingException(String grammarName, String message, ParsingException[] lastExceptions) {
        this.message = message;
        this.grammarName = grammarName;
        this.location = new Lazy<>(
                () -> Arrays.stream(lastExceptions)
                        .map(ex -> ex.location.get())
                        .max(locationComparator)
                        .orElseThrow()
        );
        causes = List.of(lastExceptions);
    }

    public String getRawMessage() {
        return getMessage();
    }

    public String markInText(String code, int context) {
        return location.get().markInText(code, context, getMessage());
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
        var otherLocation = e.location.get();
        var myLocation = this.location.get();
        return otherLocation.getFromLine() == myLocation.getFromLine() && otherLocation.getFromColumn() == myLocation.getFromColumn();
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

