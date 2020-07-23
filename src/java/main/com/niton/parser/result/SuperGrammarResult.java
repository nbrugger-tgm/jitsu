package com.niton.parser.result;

import com.niton.parser.GrammarResult;
import com.niton.parser.token.Tokenizer.AssignedToken;
import com.niton.parser.grammars.ChainGrammar;

import java.util.*;

/**
 * The result of a {@link ChainGrammar}
 *
 * Describes a row of GrammarObjects. Could also be seen as Result Container.
 * This is the result of any Grammar matching more than one thing. Very important to build a syntax tree
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class SuperGrammarResult extends GrammarResult {
	public List<GrammarResult> objects = new ArrayList<>();
	public Map<String,Integer> naming = new LinkedHashMap<>();

	public void setNaming(Map<String, Integer> naming) {
		this.naming = naming;
	}

	public void setObjects(List<GrammarResult> objects) {
		this.objects = objects;
	}

	public void name(String name, GrammarResult res){
		add(res);
		naming.put(name, objects.size()-1);
	}

	public boolean add(GrammarResult grammarResult) {
		return objects.add(grammarResult);
	}

	public List<AssignedToken> join() {
		List<AssignedToken> token = new LinkedList<>();
		for (GrammarResult object : objects) {
			token.addAll(object.join());
		}
		return token;
	}



	/**
	 * Get a named sub-object by its name
	 * @param name the name of the sub object to get
	 * @return the GrammarObject
	 */
	public <T extends GrammarResult> T getObject(String name) {
		return (T) objects.get(naming.get(name));
	}


	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(getOriginGrammarName());
		for (GrammarResult grammarObject : objects) {
			builder.append("\n   ");
			builder.append(grammarObject.toString().replaceAll("\n", "\n   "));
		}
		builder.append("\n]");
		return builder.toString();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString(int depth) {
		if (depth == 0)
			return "[" + joinTokens() + "]";
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(getOriginGrammarName());
		for (GrammarResult grammarObject : objects) {
			builder.append("\n   ");
			if (grammarObject instanceof SuperGrammarResult)
				builder.append(((SuperGrammarResult) grammarObject).toString(depth - 1).replaceAll("\n", "\n   "));
			else
				builder.append(grammarObject.toString().replaceAll("\n", "\n    "));
		}
		builder.append("\n]");
		return builder.toString();
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-30
	 */
	public String toClearString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(getOriginGrammarName());
		for (GrammarResult grammarObject : objects) {
			builder.append("\n   ");

			if (grammarObject instanceof SuperGrammarResult) {
				builder.append(((SuperGrammarResult) grammarObject).toClearString().replaceAll("\n", "\n   "));
			} else if (grammarObject instanceof TokenGrammarResult) {
				builder.append(((TokenGrammarResult) grammarObject).joinTokens());
			}
		}
		builder.append("\n]");
		return builder.toString();
	}
}
