package com.niton.parser;

import java.util.ArrayList;

import com.niton.parser.Tokenizer.AssignedToken;

/**
 * This is the SubGrammerObject Class
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class SubGrammerObject extends GrammarObject {
	public ArrayList<GrammarObject> objects = new ArrayList<>();

	public ArrayList<AssignedToken> join() {
		ArrayList<AssignedToken> token = new ArrayList<>();
		for (GrammarObject object : objects) {
			if (object instanceof SubGrammerObject)
				token.addAll(((SubGrammerObject) object).join());
			else if (object instanceof TokenGrammerObject)
				token.addAll(((TokenGrammerObject) object).tokens);
		}
		return token;
	}

	public String joinTokens() {
		StringBuilder builder = new StringBuilder();
		for (AssignedToken grammerObject : join()) {
			builder.append(grammerObject.value);
		}
		return builder.toString();
	}

	public GrammarObject getObject(String name) {
		for (GrammarObject grammerObject : objects) {
			if (grammerObject.getName().equals(name))
				return grammerObject;
		}
		return null;
	}

	public ArrayList<GrammarObject> getObjects(String name) {
		ArrayList<GrammarObject> list = new ArrayList<>();
		for (GrammarObject grammerObject : objects) {
			if (grammerObject.getName().equals(name))
				list.add( grammerObject);
		}
		return list;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(getName());
		for (GrammarObject grammerObject : objects) {
			builder.append("\n   ");
			builder.append(grammerObject.toString().replaceAll("\n", "\n   "));
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
		builder.append(getName());
		for (GrammarObject grammerObject : objects) {
			builder.append("\n   ");
			if (grammerObject instanceof SubGrammerObject)
				builder.append(((SubGrammerObject) grammerObject).toString(depth - 1).replaceAll("\n", "\n   "));
			else
				builder.append(grammerObject.toString().replaceAll("\n", "\n    "));
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
		builder.append(getName());
		for (GrammarObject grammerObject : objects) {
			builder.append("\n   ");

			if (grammerObject instanceof SubGrammerObject) {
				builder.append(((SubGrammerObject) grammerObject).toClearString().replaceAll("\n", "\n   "));
			} else if (grammerObject instanceof TokenGrammerObject) {
				builder.append(((TokenGrammerObject) grammerObject).joinTokens());
			}
		}
		builder.append("\n]");
		return builder.toString();
	}
}
