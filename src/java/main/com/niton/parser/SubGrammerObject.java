package com.niton.parser;

import java.util.ArrayList;

import com.niton.parser.Tokenizer.AssignedToken;

/**
 * The result of a {@link com.niton.parser.grammar.ChainGrammer}
 *
 * Describes a row of GrammarObjects. Could also be seen as Result Container.
 * This is the result of any Grammar matching more than one thing. Very important to build a syntax tree
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class SubGrammerObject extends GrammarObject {
	public ArrayList<GrammarObject> objects = new ArrayList<>();

	/**
	 * Collects all Tokens of underlying Grammars recursively. This leads to the original parsed text except of ignored tokens
	 * @return the ordered list of all recursive tokens
	 */
	public ArrayList<AssignedToken> join() {
		ArrayList<AssignedToken> token = new ArrayList<>();
		for (GrammarObject object : objects) {
			if (object instanceof SubGrammerObject)
				token.addAll(((SubGrammerObject) object).join());
			else if (object instanceof TokenGrammarObject)
				token.addAll(((TokenGrammarObject) object).tokens);
		}
		return token;
	}

	/**
	 * Simmilar to  {@link #join()} but joining the token values to a string
	 * @return
	 */
	public String joinTokens() {
		StringBuilder builder = new StringBuilder();
		for (AssignedToken grammerObject : join()) {
			builder.append(grammerObject.value);
		}
		return builder.toString();
	}

	/**
	 * Get a named sub-object by its name
	 * @param name the name of the sub object to get
	 * @return the GrammarObject
	 */
	public <T extends GrammarObject> T getObject(String name) {
		for (GrammarObject grammerObject : objects) {
			if(grammerObject.getName() == null)
				continue;
			if (grammerObject.getName().equals(name))
				return (T) grammerObject;
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
			} else if (grammerObject instanceof TokenGrammarObject) {
				builder.append(((TokenGrammarObject) grammerObject).joinTokens());
			}
		}
		builder.append("\n]");
		return builder.toString();
	}
}
