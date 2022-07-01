package com.niton.parser.ast;

import com.niton.parser.token.Tokenizer;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is the IngoredGrammarObject Class
 *
 * @author Nils
 * @version 2019-05-29
 */
public class IgnoredNode extends AstNode {
	public  boolean                       saveRAM = false;
	private List<Tokenizer.AssignedToken> ignored = new ArrayList<>(0);

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[IGNORED]";
	}

	public List<Tokenizer.AssignedToken> getIgnored() {
		return ignored;
	}

	public void setIgnored(List<Tokenizer.AssignedToken> ignored) {
		this.ignored = ignored;
	}

	public void add(Tokenizer.AssignedToken val) {
		if (!saveRAM) {
			ignored.add(val);
		}
	}

	@Override
	public Collection<Tokenizer.AssignedToken> join() {
		if (saveRAM) return List.of();
		return ignored;
	}

	@Override
	public ReducedNode reduce(@NonNull String name) {
		return null;
	}
}

