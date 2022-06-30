package com.niton.parser.token;

import com.niton.parser.exceptions.ParsingException;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.*;

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
	 * @throws ParsingException
	 */
	public List<AssignedToken> tokenize(String content) throws ParsingException {
		List<AssignedToken> assignedTokens = parseTokens(content);
		verifyNoOverlap(content, assignedTokens);
		fillGaps(content, assignedTokens);
		return assignedTokens;
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

	private void verifyNoOverlap(String content, List<AssignedToken> tokens)
			throws ParsingException {
		int           last      = 0;
		AssignedToken lastToken = null;
		for (var assignedToken : tokens) {
			if (assignedToken.start > last) {
				last = assignedToken.start;
			} else if (last > assignedToken.start) {
				throw overlapException(content, last, assignedToken, lastToken);
			}
			last += assignedToken.value.length();
			lastToken = assignedToken;
		}
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
			String content,
			int last,
			AssignedToken assignedToken,
			AssignedToken overlapedWith
	) {
		return new ParsingException(String.format(
				"Tokens overlapping: %s overlaps previous Token %s!" +
						" Last token ended at %d and this token startet at %d (%s)",
				assignedToken,
				overlapedWith,
				last,
				assignedToken.start,
				content.substring(max(0, last - 5), min(last + 5, content.length()))
		));
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
