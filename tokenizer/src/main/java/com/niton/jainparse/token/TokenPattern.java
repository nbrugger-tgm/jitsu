package com.niton.jainparse.token;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * A token describes a group of chars which comonly appear next to each other or single characters.
 * Works with RegEx. Features only to match when a special regex is bevore or after the main
 * expression
 *
 * @author Nils
 * @version 2019-05-27
 */
public class TokenPattern {
	private static final String TOKEN_REPLACER    = "{main}";
	private static final String LEADING_REPLACER  = "{lead}";
	private static final String TRAILING_REPLACER = "{trail}";
	private static final String TEMPLATE          = format(
			"(?<=%s)(%s)(?=%s)",
			LEADING_REPLACER,
			TOKEN_REPLACER,
			TRAILING_REPLACER
	);

	@NonNull
	private final List<TokenPattern> leading;
	@NonNull
	private final List<TokenPattern> trailing;
	@NonNull
	private       Pattern            regex;

	/**
	 * Creates an Instance of Token.java
	 *
	 * @author Nils
	 * @version 2019-05-27
	 */
	public TokenPattern(@NonNull String regex) {
		this(Pattern.compile(regex, Pattern.MULTILINE));
	}

	/**
	 * Creates an Instance of Token.java
	 *
	 * @param regex
	 *
	 * @author Nils
	 * @version 2019-05-27
	 */
	public TokenPattern(@NonNull Pattern regex) {
		this(List.of(), List.of(), regex);
	}

	/**
	 * Creates an Instance of Token.java
	 *
	 * @param leading
	 * @param trailing
	 * @param regex
	 *
	 * @author Nils
	 * @version 2019-05-27
	 */
	public TokenPattern(
			@NonNull List<TokenPattern> leading,
			@NonNull List<TokenPattern> trailing,
			@NonNull Pattern regex
	) {
		super();
		this.leading  = leading;
		this.trailing = trailing;
		this.regex    = regex;
	}

	public TokenPattern(
			@NonNull TokenPattern leading,
			@NonNull TokenPattern trailing,
			@NonNull Pattern regex
	) {
		super();
		this.leading  = List.of(leading);
		this.trailing = List.of(trailing);
		this.regex    = regex;
	}

	public TokenPattern(
			@NonNull TokenPattern leading,
			@NonNull TokenPattern trailing,
			@NonNull String regex
	) {
		super();
		this.leading  = List.of(leading);
		this.trailing = List.of(trailing);
		this.regex    = Pattern.compile(regex, Pattern.MULTILINE);
	}

	/**
	 * @return the leading
	 */
	public List<TokenPattern> getLeading() {
		return leading;
	}

	/**
	 * @param leading the leading to set
	 */
	public void setLeading(TokenPattern leading) {
		this.leading.clear();
		this.leading.add(leading);
	}

	/**
	 * @return the trailing
	 */
	public List<TokenPattern> getTrailing() {
		return trailing;
	}

	/**
	 * @param trailing the trailing to set
	 */
	public void setTrailing(TokenPattern trailing) {
		this.trailing.clear();
		this.trailing.add(trailing);
	}

	/**
	 * @return the compiled main regex (NOT contains tail/leading)
	 */
	public @NotNull Pattern getRegex() {
		return regex;
	}

	/**
	 * @param regex the regex to set
	 */
	public void setRegex(@NonNull Pattern regex) {
		this.regex = regex;
	}

	/**
	 * @param regex the regex to set
	 */
	public void setRegex(String regex) {
		this.regex = Pattern.compile(regex, Pattern.MULTILINE);
	}

	/**
	 * @param arg0
	 *
	 * @return
	 *
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	public boolean addLeading(TokenPattern arg0) {
		return leading.add(arg0);
	}

	/**
	 * @see java.util.ArrayList#clear()
	 */
	public void clearLeading() {
		leading.clear();
	}

	/**
	 * @param arg0
	 *
	 * @return
	 *
	 * @see java.util.ArrayList#remove(java.lang.Object)
	 */
	public boolean removeLeading(TokenPattern arg0) {
		return leading.remove(arg0);
	}

	/**
	 * @param arg0
	 *
	 * @return
	 *
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean addTrailing(TokenPattern arg0) {
		return trailing.add(arg0);
	}


	/**
	 * @param arg0
	 *
	 * @return
	 *
	 * @see java.util.ArrayList#remove(java.lang.Object)
	 */
	public boolean removeTrailing(TokenPattern arg0) {
		return trailing.remove(arg0);
	}

	/**
	 * @return the compiled complete regex (contains tail/leading)
	 */
	public Pattern getCompletePattern() {
		return Pattern.compile(
				TEMPLATE.replace(TOKEN_REPLACER, regex.pattern())
				        .replace(TRAILING_REPLACER, getTrailingRegex())
				        .replace(LEADING_REPLACER, getLeadingRegex()),
				Pattern.MULTILINE
		);
	}

	/**
	 * Description :
	 *
	 * @return
	 *
	 * @author Nils
	 * @version 2019-05-27
	 */
	private CharSequence getLeadingRegex() {
		return buildCondition(leading);
	}

	/**
	 * Description :
	 *
	 * @return
	 *
	 * @author Nils
	 * @version 2019-05-27
	 */
	private CharSequence getTrailingRegex() {
		return buildCondition(trailing);
	}

	private CharSequence buildCondition(List<TokenPattern> trailing) {
		if (trailing.isEmpty()) {
			return ".*";
		}
		if (trailing.size() == 1) {
			return trailing.get(0).getCompletePattern().pattern();
		} else {
			StringBuilder builder = new StringBuilder();
			for (TokenPattern tokenPattern : trailing) {
				builder.append('(');
				builder.append(tokenPattern.getCompletePattern().pattern());
				builder.append(')');
				builder.append('|');
			}
			builder.deleteCharAt(builder.length() - 1);
			return builder.toString();
		}
	}
}

