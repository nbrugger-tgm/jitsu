package com.niton.jainparse.token;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.token.Tokenizer.AssignedToken;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

/**
 * A Stream of tokens that supports stack based navigation
 */
class ListTokenStream<T extends Enum<T> & Tokenable> implements TokenStream<T> {
    private final LinkedList<Integer> levelIndexes = new LinkedList<>();
    private final LinkedList<Integer> levelLines = new LinkedList<>();
    private final LinkedList<Integer> levelColumns = new LinkedList<>();
    private final LinkedList<Integer> levelLastColumns = new LinkedList<>();
    private final LinkedList<Integer> levelLastLines = new LinkedList<>();
    private final List<AssignedToken<T>> tokens;
    @Getter
    @Setter
    private int recursionLevelLimit = 500;
    /**
     * The token at the current index, if it is not null it will be used instead of the token at the current index
     * null means not cached yet -> set to null when index changes
     */
    @Nullable
    private AssignedToken<T> nextTokenCache;

    public ListTokenStream(List<AssignedToken<T>> tokens) {
        this.tokens = tokens;
        levelIndexes.push(0);
        levelLines.push(1);
        levelColumns.push(1);
        levelLastColumns.push(0);
        levelLastLines.push(1);
    }

    /**
     * Returns the current marked assigned token and jumps one further
     */
    @Override
    public Optional<AssignedToken<T>> nextOptional() {
        try {
            return Optional.of(next());
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

	public @NotNull AssignedToken<T> next() throws IndexOutOfBoundsException {
		return nextOffset(0);
	}

	/**
     * in the context of {@link #lastConsumedLocation()}, skipped tokens are also 'consumed'
     *
	 * @param offset 0: read the next token, n>0: skip n tokens before reading
	 */
	private @NotNull AssignedToken<T> nextOffset(int offset) {
        final var nextTokenIndex = index();
        var tkn = nextTokenCache != null ? nextTokenCache : tokens.get(nextTokenIndex + offset);
        nextTokenCache = null;
		var newLines = newlineCount(tkn.getValue());
		for (int i = 0; i < offset; i++) {
			newLines += newlineCount(tokens.get(nextTokenIndex + i).getValue());
		}
		if (newLines > 0) {
            @SuppressWarnings("DataFlowIssue") //we know there are newlines so null cannot happen
            var consumedInLastLine = offset == 0 ? findLenghtOfLastLine(tkn.getValue()) : findLengthOfLastLine(nextTokenIndex, nextTokenIndex + offset);

            if (consumedInLastLine == 0) {
			    if(newLines == 1 && offset == 0) {
                    //This is just an optimization branch - the else works in every scenario (but uses slightly expensive findLengthOfLine
                    setLastConsumedPosition(getColumn()+ tkn.getValue().lastIndexOf('\n'), getLine());
                } else {
			        var lengthOfLineBeforeLastLine = findLengthOfLine(nextTokenIndex, nextTokenIndex + offset, 1);
			        setLastConsumedPosition(lengthOfLineBeforeLastLine+1, getLine() + newLines - 1);
			    }
            }
            setCurrentPosition(consumedInLastLine+1, getLine()+newLines);
		} else {
            levelColumns.set(0, getColumn()+ tkn.getValue().length());
        }
		levelIndexes.set(0, nextTokenIndex + offset + 1);
		return tkn;
	}

	private int findLengthOfLastLine(int from, int to) {
		return findLengthOfLine(from, to, 0);
	}

    private void setLastConsumedPosition(int column, int line) {
        levelLastColumns.set(0, column);
        levelLastLines.set(0, line);
    }
    private void setCurrentPosition(int column, int line) {
        levelLines.set(0, line);
        levelColumns.set(0, column);
    }

    /**
     * Finds the length of the 'offset' to last line in the given range
     * <p><b>Example</b>: given the following text in the range
     * <pre>
     *     123
     *     1234
     *
     *     12
     * </pre>
     * <ul>
     *     <li>{@code findLengthOfLine(offset=0) => 2} 0 means last line which is "12" so 2 chars</li>
     *     <li>{@code findLengthOfLine(offset=1) => 0} 1 means 1st to last line which is "" so 0 chars</li>
     *     <li>{@code findLengthOfLine(offset=2) => 4} 2 means 2nd to last line which is "1234" so 4 chars</li>
     * </ul>
     *
     * </p>
     * @param fromToken (inclusive) the lower-index token, start of the search area
     * @param toToken (inclusive) the higher-index token, end of the search area
     * @param offset ignore this many lines from the back the range
     * @return lenght of last line in the given token range
     */
    private int findLengthOfLine(int fromToken, int toToken, int offset) {
        int lastLineLen = 0;
        for (int i = toToken; i >= fromToken; i--) {
            var token = tokens.get(i);
            var value = token.getValue();

            var lastNewline = value.length();
            while(offset > 0 && (lastNewline = value.lastIndexOf('\n', lastNewline - 1)) != -1)
                offset --;
            if(lastNewline == -1) continue;
            /*offset used up*/
            value = value.substring(0, lastNewline);

            var tokenLastLineLength = findLenghtOfLastLine(value);
            if(tokenLastLineLength == null) {
                lastLineLen += value.length();
            } else {
                lastLineLen += tokenLastLineLength;
                break;
            }
        }
        return lastLineLen;
    }

    private @Nullable Integer findLenghtOfLastLine(String text) {
        var linebreak = text.lastIndexOf('\n');
        if(linebreak == -1) return null;
        return text.length() - (linebreak + 1);
    }

    @Override
	public void skip(int n) {
		if (n == 0) return;
		if (n < 0) throw new IllegalArgumentException("Cannot skip negative amounts");
		nextOffset(n - 1);
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
        levelLastColumns.push(getLastConsumedColumn());
        levelLastLines.push(getLastConsumedLine());
        if (levelIndexes.size() >= recursionLevelLimit) {
            throw new IllegalStateException("Max Recursions reached (" + recursionLevelLimit + ") [" + index() + "]");
        }
    }


    private Integer getLastConsumedColumn() {
        return levelLastColumns.peek();
    }

    private Integer getLastConsumedLine() {
        return levelLastLines.peek();
    }

    /**
     * Removes the current stack frame and applies the index to  the previous stack frame
     */
    @Override
    public void commit() {
        if(levelIndexes.size() == 1) {
            throw new IndexOutOfBoundsException("No stack frame to commit");
        }
        index(levelIndexes.pop());
        setCurrentPosition(levelColumns.pop(), levelLines.pop());
        setLastConsumedPosition(levelLastColumns.pop(), levelLastLines.pop());
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
        levelLastColumns.pop();
        levelLastLines.pop();
        nextTokenCache = null;
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
        if(nextTokenCache == null) {
            if(!hasNext()) {
                return Optional.empty();
            }
            nextTokenCache = tokens.get(index());
        }
        return Optional.of(nextTokenCache);
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
        if(getColumn() == 1) {
            var line = getLine();
            if(line == 1) throw new IllegalStateException("Nothing was consumed yet");
            return Location.oneChar(getLastConsumedLine(), getLastConsumedColumn());
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

    private static int lastIndexOf(String string, char c, int skip) {
        var i = string.length();
        while((i = string.lastIndexOf(c, i-1)) != -1 && skip > 0) skip --;
        return i;
    }

    private static int newlineCount(String s) {
        var count = 0;
        var i = -1;
        while((i = s.indexOf('\n', i+1)) != -1) count ++;
        return count;
    }
}
