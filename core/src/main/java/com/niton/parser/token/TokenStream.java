package com.niton.parser.token;

import com.niton.parser.exceptions.ParsingException;
import com.niton.parser.token.Tokenizer.AssignedToken;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

/**
 * A Stream of tokens that supports stack based navigation
 */
public class TokenStream {
	private final LinkedList<Integer> levelIndexes        = new LinkedList<>();
	private final List<AssignedToken> tokens;
	@Getter
	@Setter
	private int recursionLevelLimit = 500;

	public TokenStream(List<AssignedToken> tokens) {
		this.tokens = tokens;
		levelIndexes.push(0);
	}

	/**
	 * Returns the current marked assigned token and jumps one further
	 */
	public AssignedToken next() {
		try {
			var tkn = tokens.get(index());
			increase();
			return tkn;
		} catch (IndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("No more tokens available");
		}
	}

	/**
	 * @return the index of the token you are at
	 */
	public int index() {
		return levelIndexes.get(0);
	}

	/**
	 * Marks the next token as consumed, <b>does not check, if there are more tokens</b>
	 */
	protected final void increase() {
		levelIndexes.set(0, index() + 1);
	}

	/**
	 * Create a new stack frame. The index is set to the current index
	 */
	public void elevate() {
		levelIndexes.push(index());
		if (levelIndexes.size() >= recursionLevelLimit) {
			throw new IllegalStateException("Max Recursions reached (" + recursionLevelLimit + ") ["+index()+"]");
		}
	}

	/**
	 * Removes the current stack frame and applies the index to  the previous stack frame
	 */
	public void commit() {
		int val = levelIndexes.pop();
		index(val);
	}


	public void index(int index) {
		levelIndexes.set(0, index);
	}

	/**
	 * removes the current stack frame and rolls the index back to the previous stack frame
	 */
	public void rollback() {
		levelIndexes.pop();
	}

	public AssignedToken get(int index) {
		return tokens.get(index);
	}

	public int level() {
		return levelIndexes.size()-1;
	}

	public int size() {
		return tokens.size();
	}

	public String getPreviousTokens(int count) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < count; i++) {
			builder.append(tokens.get(index()-i).getValue());
		}
		return builder.toString();
	}

	@Override
	public String toString() {
		return levelIndexes.toString();
	}

	public boolean hasNext() {
		return index() < size();
	}
}
