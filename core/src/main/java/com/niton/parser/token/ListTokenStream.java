package com.niton.parser.token;

import com.niton.parser.token.Tokenizer.AssignedToken;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

/**
 * A Stream of tokens that supports stack based navigation
 */
public class ListTokenStream implements TokenStream {
    private final LinkedList<Integer> levelIndexes = new LinkedList<>();
    private final LinkedList<Integer> levelLines = new LinkedList<>();
    private final LinkedList<Integer> levelColumns = new LinkedList<>();
    private final List<AssignedToken> tokens;
    @Getter
    @Setter
    private int recursionLevelLimit = 500;

    public ListTokenStream(List<AssignedToken> tokens) {
        this.tokens = tokens;
        levelIndexes.push(0);
        levelLines.push(1);
        levelColumns.push(1);
    }

    /**
     * Returns the current marked assigned token and jumps one further
     */
    @Override
    public AssignedToken next() {
        try {
            var tkn = tokens.get(index());
            if (tkn.getValue().contains("\n")) {
                levelLines.set(0, levelLines.get(0) + 1);
                levelColumns.set(0, 1);
            } else {
                levelColumns.set(0, levelColumns.get(0) + tkn.getValue().length());
            }
            levelIndexes.set(0, index() + 1);
            return tkn;
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("No more tokens available");
        }
    }

    /**
     * @return the index of the token you are at
     */
    @Override
    public int index() {
        return levelIndexes.get(0);
    }

    /**
     * Create a new stack frame. The index is set to the current index
     */
    @Override
    public void elevate() {
        levelIndexes.push(index());
        levelColumns.push(getColumn());
        levelLines.push(getLine());
        if (levelIndexes.size() >= recursionLevelLimit) {
            throw new IllegalStateException("Max Recursions reached (" + recursionLevelLimit + ") [" + index() + "]");
        }
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
    }

    AssignedToken get(int index) {
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
    public AssignedToken peek() {
        return tokens.get(index());
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
        return levelIndexes.toString();
    }

    @Override
    public boolean hasNext() {
        return index() < size();
    }

    @Override
    public TokenStream subStream(int startIndex, int endIndex) {
        if (startIndex > endIndex)
            throw new IllegalArgumentException("Start index must be smaller than end index");
        var subStream = new ListTokenStream(tokens.subList(0, endIndex));
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
    public void skip(int i) {
        index(index() + i);
    }
}
