package com.niton.jainparse.token;

import com.niton.jainparse.api.Location;
import com.niton.jainparse.api.ParsingResult;
import com.niton.jainparse.exceptions.ParsingException;
import lombok.AllArgsConstructor;
import lombok.Data;
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
public class Tokenizer {
	private final Map<String, TokenPattern> tokens    = new HashMap<>();
	private       boolean                   ignoreEOF = false;

	/**
	 * An assigned token defines which token matches the part of the string stored in {@link
	 * AssignedToken#value}
	 */
	@Data
	@AllArgsConstructor
	public static class AssignedToken {
		private String value;
		private String name;
		private int    start;

		public AssignedToken(String value, String name) {
			this.value = value;
			this.name  = name;
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
			return value + " (" + name + ")";
		}
	}

	/**
	 * Creates a tokenizer based on {@link DefaultToken}
	 */
	public Tokenizer() {
		this(DefaultToken.values());
	}

	/**
	 * {@link Tokenizer#add(Tokenable[])}
	 *
	 * @param tokens
	 */
	public Tokenizer(Tokenable... tokens) {
		setTokens(tokens);
	}

	public Tokenizer(List<Tokenable> tokens) {
		for (Tokenable t : tokens) {
			this.tokens.put(t.name(), new TokenPattern(t.pattern()));
		}
	}

	public Map<String, TokenPattern> getTokens() {
		return Collections.unmodifiableMap(tokens);
	}

	public void setTokens(Tokenable... tokens) {
		for (Tokenable t : tokens) {
			this.tokens.put(t.name(), new TokenPattern(t.pattern()));
		}
	}

	/**
	 * Tokenizes the string. Maps each part of the string to a token. If no token in the tokenizer
	 * matches a certain part UNDEFINED is used
	 *
	 * @param content the string to map to tokens
	 *
	 * @return a list of assigned tokens.
	 *
	 * @
	 */
	public ParsingResult<List<AssignedToken>> tokenize(String content) {
		List<AssignedToken> assignedTokens = parseTokens(content);
		var err = verifyNoOverlap(assignedTokens);
		if(err != null)
			return ParsingResult.error(err);
		fillGaps(content, assignedTokens);
		return ParsingResult.ok(assignedTokens);
	}

	private List<AssignedToken> parseTokens(String content) {
		List<AssignedToken> parsed = new LinkedList<>();
		for (String tokenName : this.tokens.keySet()) {
			TokenPattern t = this.tokens.get(tokenName);
			if (ignoreEOF && t.getRegex().pattern().equals(DefaultToken.EOF.regex)) {
				continue;
			}
			Pattern p = t.getCompletePattern();
			Matcher m = p.matcher(content);
			while (m.find()) {
				AssignedToken res = new AssignedToken();
				res.name  = tokenName;
				res.value = m.group();
				res.start = m.start();
				parsed.add(res);
			}
		}
		parsed.sort(Comparator.comparingInt((AssignedToken o) -> o.start));
		return parsed;
	}

	private @Nullable ParsingException verifyNoOverlap(List<AssignedToken> tokens) {
		int           last      = 0;
		AssignedToken lastToken = null;
		var line = 1;
		var col = 1;

		for (var assignedToken : tokens) {
			if (assignedToken.start > last) {
				last = assignedToken.start;
			} else if (last > assignedToken.start) {
				return overlapException(assignedToken, lastToken,line,col);
			}
			last += assignedToken.value.length();
			lastToken = assignedToken;
			if(assignedToken.value.contains("\n")) {
				line++;
				col = 1;
			}else{
				col += assignedToken.value.length();
			}
		}
		return null;
	}

	private void fillGaps(String content, List<AssignedToken> tokens) {
		int                 last      = 0;
		List<AssignedToken> undefined = new ArrayList<>(tokens.size());
		for (var assignedToken : tokens) {
			if (assignedToken.start > last) {
				var undef = new AssignedToken(
						content.substring(last, assignedToken.start),
						"UNDEFINED"
				);
				undef.start = last;
				undefined.add(undef);
				last += undef.value.length();
			}
			last += assignedToken.value.length();
		}
		if (last < content.length()) {
			undefined.add(new AssignedToken(
					content.substring(last),
					"UNDEFINED"
			));
		}
		tokens.addAll(undefined);
		tokens.sort(Comparator.comparingInt((AssignedToken o) -> o.start));
	}

	private ParsingException overlapException(
			AssignedToken assignedToken,
			AssignedToken overlapedWith,
			int line,
			int col
	) {
		return new ParsingException("[Tokenizer]", String.format(
				"Tokens overlapping: %s overlaps previous Token %s!",
				assignedToken,
				overlapedWith
		), Location.of(line,col, line, col + assignedToken.value.length()));
	}

	/**
	 * Adds tokens the Tokenizer understands
	 *
	 * @param tokens the tokens to add
	 */
	public void add(Tokenable... tokens) {
		for (Tokenable t : tokens) {
			this.tokens.put(t.name(), new TokenPattern(t.pattern()));
		}
	}

	public boolean isIgnoreEOF() {
		return ignoreEOF;
	}

	public void setIgnoreEOF(boolean ignoreEOF) {
		this.ignoreEOF = ignoreEOF;
	}
}
