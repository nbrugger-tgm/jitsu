package com.niton.jainparse.token;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A Stream of tokens that supports stack based navigation
 */
class ListTokenStream<T extends Enum<T> & Tokenable> implements TokenStream<T> {
    private final LinkedList<Integer> levelIndexes = new LinkedList<>();
    private final LinkedList<Integer> levelLines = new LinkedList<>();
    private final LinkedList<Integer> levelColumns = new LinkedList<>();
    private final LinkedList<Integer> levelLastLineColumns = new LinkedList<>();
    private final List<AssignedToken<T>> tokens;
    @Getter
    @Setter
    private int recursionLevelLimit = 500;
    /**
     * The token at the current index, if it is not null it will be used instead of the token at the current index
     * null means not cached yet -> set to null when index changes
     */
    @Nullable
    private AssignedToken<T> currentCache;

    public ListTokenStream(List<AssignedToken<T>> tokens) {
        this.tokens = tokens;
        levelIndexes.push(0);
        levelLines.push(1);
        levelColumns.push(1);
        levelLastLineColumns.push(0);
    }

    /**
     * Returns the current marked assigned token and jumps one further
     */
    @Override
    public Optional<AssignedToken<T>> nextOptional() {
        try {
            var tkn = currentCache != null ? currentCache : tokens.get(index());
            currentCache = null;
            if (tkn.getValue().contains("\n")) {
                levelLines.set(0, levelLines.get(0) + 1);
                levelLastLineColumns.set(0, levelColumns.get(0));
                levelColumns.set(0, 1);
            } else {
                levelColumns.set(0, levelColumns.get(0) + tkn.getValue().length());
            }
            levelIndexes.set(0, index() + 1);
            return Optional.of(tkn);
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    /**
     * @return the index of the token you are at
     */
    @Override
    public int index() {
        return levelIndexes.getFirst();
    }

    /**
     * Create a new stack frame. The index is set to the current index
     */
    @Override
    public void elevate() {
        levelIndexes.push(index());
        levelColumns.push(getColumn());
        levelLines.push(getLine());
        levelLastLineColumns.push(getLastLineColumn());
        if (levelIndexes.size() >= recursionLevelLimit) {
            throw new IllegalStateException("Max Recursions reached (" + recursionLevelLimit + ") [" + index() + "]");
        }
    }

    private Integer getLastLineColumn() {
        return levelLastLineColumns.get(0);
    }

    /**
     * Removes the current stack frame and applies the index to  the previous stack frame
     */
    @Override
    public void commit() {
        if(levelIndexes.size() == 1) {
            throw new IndexOutOfBoundsException("No stack frame to commit");
        }
        int val = levelIndexes.pop();
        index(val);
        levelColumns.set(0, levelColumns.pop());
        levelLines.set(0, levelLines.pop());
        levelLastLineColumns.set(0, levelLastLineColumns.pop());
    }

    /**
     * Manual jumping invalidates {@link #getColumn()} and {@link #getLine()}
     */
    void index(int index) {
        levelIndexes.set(0, index);
    }

    /**
     * removes the current stack frame and rolls the index back to the previous stack frame
     */
    @Override
    public void rollback() {
        if(levelIndexes.size() == 1) {
            throw new IndexOutOfBoundsException("No stack frame to rollback");
        }
        levelIndexes.pop();
        levelColumns.pop();
        levelLines.pop();
        levelLastLineColumns.pop();
        currentCache = null;
    }

    AssignedToken<T> get(int index) {
        return tokens.get(index);
    }

    @Override
    public int level() {
        return levelIndexes.size() - 1;
    }

    @Override
    public int size() {
        return tokens.size();
    }

    @Override
    public Optional<AssignedToken<T>> peekOptional() {
        if(currentCache == null) {
            if(!hasNext()) {
                return Optional.empty();
            }
            currentCache = tokens.get(index());
        }
        return Optional.of(currentCache);
    }

    @Override
    public String getPreviousTokens(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(tokens.get(index() - i).getValue());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        if(!hasNext()) return "ListTokenStream[fully consumed]";
        return "["+levelIndexes.stream().map(idx -> {
            StringBuilder builder = new StringBuilder();
            if(idx > 0) {
                builder.append(tokens.get(idx - 1).getValue());
            }
            builder.append('[')
                    .append(tokens.get(idx).getValue())
                    .append(']');
            if(idx < tokens.size() - 1) {
                builder.append(tokens.get(idx + 1).getValue());
            }
            return builder.toString();
        }).collect(Collectors.joining(", "))+"]";
    }

    @Override
    public boolean hasNext() {
        return index() < size();
    }

    @Override
    public TokenStream<T> subStream(int startIndex, int endIndex) {
        if (startIndex > endIndex)
            throw new IllegalArgumentException("Start index must be smaller than end index");
        var subStream = new ListTokenStream<>(tokens.subList(0, endIndex));
        subStream.index(startIndex);
        return subStream;
    }

    @Override
    public int getLine() {
        return levelLines.get(0);
    }

    @Override
    public int getColumn() {
        return levelColumns.get(0);
    }

    @Override
    public Location currentLocation() {
        return Location.oneChar(getLine(), getColumn());
    }

    @Override
    public Location lastConsumedLocation() {
        if(getColumn() == 0) {
            return Location.oneChar(getLine()-1, getLastLineColumn());
        }
        return Location.oneChar(getLine(), getColumn()-1);
    }

    @Override
    public AssignedToken<T> splice(int i) {
        var next = peekOptional();
        if(next.isEmpty()) return null;
        var value = next.get().getValue();
        if(value.length() < i) return null;
        if(value.length() == i) {
            return next();
        }
        var toSplit = next();
        var split = new AssignedToken<>(toSplit.getValue().substring(0, i), toSplit.getType());
        split.setStart(toSplit.getStart());
        toSplit.setStart(toSplit.getStart() + i);
        toSplit.setValue(toSplit.getValue().substring(i));
        tokens.add(index()-1, split);
        return split;
    }
}
