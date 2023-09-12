package com.niton.parser.token;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import static java.lang.Math.max;

public interface Location {
    public static final Comparator<Location> byStart = Comparator.comparingInt(Location::getFromLine)
            .thenComparingInt(Location::getFromColumn)
            .thenComparingInt(Location::getToLine)
            .thenComparingInt(Location::getToColumn);

    public static final Comparator<Location> byEnd = Comparator.comparingInt(Location::getToLine)
            .thenComparingInt(Location::getToColumn)
            .thenComparingInt(Location::getFromLine)
            .thenComparingInt(Location::getFromColumn);

    default String format() {
        if (getFromLine() == getToLine()) {
            if (getFromColumn() == getToColumn())
                return String.format("%d:%d", getFromLine(), getFromColumn());
            else
                return String.format("%d:%d-%d", getFromLine(), getFromColumn(), getToColumn());
        } else
            return String.format("%d:%d-%d:%d", getFromLine(), getFromColumn(), getToLine(), getToColumn());
    }

    @NotNull
    static Location of(int startLine, int startColumn, int endLine, int endColumn) {
        return new Location() {
            @Override
            public int getFromLine() {
                return startLine;
            }

            @Override
            public int getFromColumn() {
                return startColumn;
            }

            @Override
            public int getToLine() {
                return endLine;
            }

            @Override
            public int getToColumn() {
                return endColumn;
            }

            @Override
            public String toString() {
                return format();
            }
        };
    }

    @NotNull
    static Location range(Location from, Location to) {
        return of(from.getFromLine(), from.getFromColumn(), to.getToLine(), to.getToColumn());
    }

    static Location oneChar(int line, int column) {
        return of(line, column, line, column);
    }

    int getFromLine();

    int getFromColumn();

    int getToLine();

    int getToColumn();

    /**
     * Marks this position in the given text
     * by underlining it in the format of {@code ^------^} or {@code ^-^-> description} depending on the size of the description and the selected area
     *
     * @param text        the text to mark this location in
     * @param context     the number of lines to show before and after the marked area
     * @param description a description that describes what is marked. If no description is needed use {@code null}
     * @return the string containing the marked text
     */
    default String markInText(String text, int context, @Nullable String description) {
        //columns are human readable -> 1 based
        int fromLine = getFromLine() - 1;
        int fromColumn = getFromColumn() - 1;
        int toLine = getToLine() - 1;
        int toColumn = getToColumn() - 1;

        String[] lines = text.split("\n");
        StringBuilder builder = new StringBuilder();
        int startLine = max(0, fromLine - context);
        int endLine = Math.min(lines.length, toLine + context);
        for (int i = startLine; i < endLine; i++) {
            String line = lines[i];
            builder.append(line).append("\n");
            if (i == fromLine) {
                //Tdoo teavm
                builder.append(" ".repeat(fromColumn)).append("^");
                if (fromLine == toLine) {
                    //Tdoo teavm
                    builder.append("-".repeat(max(0, toColumn - fromColumn - 2)));
                    if (toColumn - fromColumn > 1) builder.append("^");
                    if (description != null) {
                        builder.append("-> ").append(description);
                    }
                } else {
                    //Tdoo teavm
                    builder.append("-".repeat(line.length() - fromColumn));
                }
                builder.append("\n");
            } else if (i > fromLine && i < toLine) {
                //Tdoo teavm
                builder.append("-".repeat(line.length())).append("\n");
            } else if (i == toLine) {
                //Tdoo teavm
                builder.append("-".repeat(max(0,toColumn - 1))).append("^");
                if (description != null) {
                    builder.append("-> ").append(description);
                }
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    default Location minusChar(int i) {
        var isOneChar = getFromColumn() == getToColumn();
        return of(getFromLine(), isOneChar ? getFromColumn() - 1 : getFromColumn(), getToLine(), getToColumn() - i);
    }

    default Location minusLine(int i) {
        return of(getFromLine(), getFromColumn(), getToLine() - i, getToColumn());
    }

    @NotNull
    default Location rangeTo(@NotNull Location parameterLoc) {
        return range(this, parameterLoc);
    }

    default Location fromChar() {
        return Location.oneChar(getFromLine(), getFromColumn());
    }
}
