package com.niton.parser.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.niton.parser.GrammarObject;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;
import com.niton.parser.Tokens;

/**
 * Used to build a grammar<br>
 * Tests all Grammars in the chain after each other
 * 
 * @author Nils
 * @version 2019-05-28
 */
public class ChainGrammer extends Grammar {
	ArrayList<Grammar> chain = new ArrayList<>();

	/**
	 * @throws ParsingException
	 * @see com.niton.parser.check.Grammar#check(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
		int pos = index();
		SubGrammerObject gObject = new SubGrammerObject();
		gObject.setName(getName());
		for (Grammar grammer : chain) {
			gObject.objects.add(grammer.check(tokens, pos));
			pos = grammer.index();
		}
		index(pos);
		return gObject;
	}

	/**
	 * @see com.niton.parser.check.Grammar#getGrammarObjectType()
	 */
	@Override
	public Class<? extends GrammarObject> getGrammarObjectType() {
		return SubGrammerObject.class;
	}

	public ChainGrammer name(String text) {
		this.setName(text);
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param number
	 * @return
	 */
	public ChainGrammer matchToken(Tokens number, String name) {
		return matchToken(number.name(), name);
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 * @param token
	 * @return
	 */
	public ChainGrammer matchToken(String token, String name) {
		chain.add(new TokenGrammer(token, name));
		return this;
	}

	public ChainGrammer match(Grammar g, String name) {
		chain.add(new GrammarMatchGrammer(g, name));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param whitespace
	 * @return
	 */
	public ChainGrammer ignoreToken(Tokens whitespace) {
		return ignoreToken(whitespace.name());
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param name2
	 * @return
	 */
	public ChainGrammer ignoreToken(String name2) {
		chain.add(new IgnoreTokenGrammer(name2));
		return this;
	}

	public ChainGrammer ignore(Grammar g) {
		chain.add(new IgnoreGrammer(g));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param multiplicator
	 * @param minus
	 * @param plus
	 * @param slash
	 * @return
	 */
	public ChainGrammer matchAnyToken(String name, Tokens... tokens) {
		return matchAnyToken(name, Arrays.stream(tokens).map(new Function<Tokens, String>() {

			@Override
			public String apply(Tokens t) {
				return t.name();
			}
		}).collect(Collectors.toList()).toArray(new String[tokens.length]));
	}

	public ChainGrammer matchAnyToken(String name, String... tokens) {
		chain.add(new MultiTokenGrammer(tokens, name));
		return this;
	}

	public ChainGrammer matchAny(String name, Grammar... tokens) {
		chain.add(new MultiGrammer(tokens, name));
		return this;
	}

	public ChainGrammer repeatMatch(Grammar expression, String name) {
		chain.add(new RepeatGrammer(expression, name));
		return this;
	}

	/**
	 * <b>Description :</b><br>
	 * 
	 * @author Nils Brugger
	 * @version 2019-06-05
	 * @param bracketOpen
	 * @return
	 */
	public ChainGrammer repeatMatchToken(Tokens bracketOpen, String name) {
		return repeatMatchToken(bracketOpen.name(), name);
	}

	/**
	 * <b>Description :</b><br>
	 * 
	 * @author Nils Brugger
	 * @version 2019-06-05
	 * @param bracketOpen
	 * @return
	 */
	public ChainGrammer repeatMatchToken(String bracketOpen, String name) {
		chain.add(new RepeatTokenGrammer(bracketOpen, name));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param token
	 * @param name
	 * @return
	 */
	public ChainGrammer anyExcept(String token, String name) {
		chain.add(new AnyExceptTokenGrammer(token, name));
		return this;
	}

	public ChainGrammer anyExcept(Tokens token, String name) {
		return anyExcept(token.name(), name);
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param value
	 * @return
	 */
	public ChainGrammer matchOptional(Grammar value, String name) {
		chain.add(new OptinalGrammer(value, name));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param token
	 * @return
	 */
	public ChainGrammer matchOptional(Tokens token, String name) {
		return matchOptional(token.name(), name);
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param value
	 * @return
	 */
	public ChainGrammer matchOptional(String token, String name) {
		chain.add(new OptinalTokenGrammer(token, name));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param number
	 * @return
	 */
	public ChainGrammer matchToken(Tokens number) {
		return matchToken(number.name(), null);
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 * @param token
	 * @return
	 */
	public ChainGrammer matchToken(String token) {
		chain.add(new TokenGrammer(token, null));
		return this;
	}

	public ChainGrammer match(Grammar g) {
		chain.add(new GrammarMatchGrammer(g, null));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param multiplicator
	 * @param minus
	 * @param plus
	 * @param slash
	 * @return
	 */
	public ChainGrammer matchAnyToken(Tokens... tokens) {
		return matchAnyToken(null, Arrays.stream(tokens).map(new Function<Tokens, String>() {

			@Override
			public String apply(Tokens t) {
				return t.name();
			}
		}).collect(Collectors.toList()).toArray(new String[tokens.length]));
	}

	public ChainGrammer matchAnyToken(String... tokens) {
		chain.add(new MultiTokenGrammer(tokens, null));
		return this;
	}

	public ChainGrammer matchAny(Grammar... tokens) {
		chain.add(new MultiGrammer(tokens, null));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param expression
	 * @return
	 */
	public ChainGrammer repeatMatch(Grammar expression) {
		return repeatMatch(expression, null);
	}

	/**
	 * <b>Description :</b><br>
	 * 
	 * @author Nils Brugger
	 * @version 2019-06-05
	 * @param bracketOpen
	 * @return
	 */
	public ChainGrammer repeatMatchToken(Tokens bracketOpen) {
		return repeatMatchToken(bracketOpen.name(), null);
	}

	/**
	 * <b>Description :</b><br>
	 * 
	 * @author Nils Brugger
	 * @version 2019-06-05
	 * @param bracketOpen
	 * @return
	 */
	public ChainGrammer repeatMatchToken(String bracketOpen) {
		chain.add(new RepeatTokenGrammer(bracketOpen, null));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param token
	 * @param name
	 * @return
	 */
	public ChainGrammer anyExcept(String token) {
		chain.add(new AnyExceptTokenGrammer(token, null));
		return this;
	}

	public ChainGrammer anyExcept(Tokens token) {
		return anyExcept(token.name(), null);
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param value
	 * @return
	 */
	public ChainGrammer matchOptional(Grammar value) {
		chain.add(new OptinalGrammer(value, null));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param token
	 * @return
	 */
	public ChainGrammer matchOptional(Tokens token) {
		return matchOptional(token.name(), null);
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param value
	 * @return
	 */
	public ChainGrammer matchOptional(String token) {
		chain.add(new OptinalTokenGrammer(token, null));
		return this;
	}

	/**
	 * @return the chain
	 */
	public ArrayList<Grammar> getChain() {
		return chain;
	}
}
