package com.niton.parser.grammar.types;

import com.niton.parser.grammar.api.Grammar;
import com.niton.parser.grammar.api.GrammarReference;
import com.niton.parser.grammar.matchers.AnyOfMatcher;
import com.niton.parser.ast.SwitchNode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;

/**
 * Checks agains all given Grammars syncron and returns the first matching
 *
 * @author Nils
 * @version 2019-05-29
 */
@Getter
@Setter
public class MultiGrammar extends Grammar<SwitchNode> implements GrammarReference{
	private Grammar<?>[] grammars;

	public MultiGrammar(Grammar<?>[] grammars) {
		this.grammars = grammars;
	}

	/**
	 * @see Grammar#createExecutor()
	 */
	@Override
	public AnyOfMatcher createExecutor() {
		return new AnyOfMatcher(this);
	}

	@Override
	public MultiGrammar or(Grammar<?>... alternatives) {
		var combined = new Grammar<?>[alternatives.length + grammars.length];

		System.arraycopy(grammars, 0, combined, 0, grammars.length);
		System.arraycopy(alternatives, 0, combined, grammars.length, alternatives.length);
		this.grammars = combined;
		return this;
	}

	@Override
	public @Nullable Grammar<?> get(String key) {
		return Arrays.stream(grammars).filter(g -> g.getName().equals(key)).findFirst().orElse(null);
	}

	@Override
	public Set<String> grammarNames() {
		return Arrays.stream(grammars).map(Grammar::getName).collect(java.util.stream.Collectors.toSet());
	}
}
