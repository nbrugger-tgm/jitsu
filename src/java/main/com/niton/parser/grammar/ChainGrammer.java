package com.niton.parser.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;
import com.niton.parser.Tokens;
import com.niton.parser.grammar.exectors.ChainExecutor;
import com.niton.parser.grammar.exectors.GrammarExecutor;
import com.niton.parser.grammar.exectors.OptionalTokenExecutor;

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
	 * @see com.niton.parser.grammar.Grammar#getGrammarObjectType()
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
	public ChainGrammer matchToken(Tokenable number, String name) {
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
		chain.add(new GrammarMatchGrammer(g.getName(), name));
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
	public ChainGrammer ignoreToken(Tokenable whitespace) {
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
		chain.add(new IgnoreGrammer(g.getName()));
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
	public ChainGrammer matchAnyToken(String name, Tokenable... tokens) {
		return matchAnyToken(name, Arrays.stream(tokens).map(new Function<Tokenable, String>() {

			@Override
			public String apply(Tokenable t) {
				return t.name();
			}
		}).collect(Collectors.toList()).toArray(new String[tokens.length]));
	}

	public ChainGrammer matchAnyToken(String name, String... tokens) {
		chain.add(new MultiTokenGrammer(tokens, name));
		return this;
	}

	public ChainGrammer matchAny(String name, Grammar... tokens) {
		chain.add(new MultiGrammer(Arrays.stream(tokens).map((a)->a.getName()).collect(Collectors.toList()).toArray(new String[tokens.length]), name));
		return this;
	}

	public ChainGrammer repeatMatch(Grammar expression, String name) {
		chain.add(new RepeatGrammer(expression.getName(), name));
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
	public ChainGrammer repeatMatchToken(Tokenable bracketOpen, String name) {
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
	
	public ChainGrammer anyExceptOneOf(String name,String... tokens) {
		chain.add(new AnyExceptMultiTokenGrammer(tokens, name));
		return this;
	}

	public ChainGrammer anyExcept(Tokenable token, String name) {
		return anyExcept(token.name(), name);
	}
	public ChainGrammer anyExceptOneOf(String name,Tokenable... tokens) {
		return anyExceptOneOf(name,Arrays.stream(tokens).map((a)->a.name()).collect(Collectors.toList()).toArray(new String[tokens.length]));
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
		chain.add(new OptinalGrammer(value.getName(), name));
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
	public ChainGrammer matchTokenOptional(Tokenable token, String name) {
		return matchTokenOptional(token.name(), name);
	}

	public ChainGrammer matchTokenOptional(String token, String name) {
		chain.add(new OptionalTokenGrammer(token, name));
		return this;
	}
	/*
	 * +---------------+
	 * | WITHOUT NAMES |
	 * +---------------+
	 */
	
	
	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param number
	 * @return
	 */
	public ChainGrammer matchToken(Tokenable number) {
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
	public ChainGrammer matchAnyToken(Tokenable... tokens) {
		return matchAnyToken(null, Arrays.stream(tokens).map(new Function<Tokenable, String>() {

			@Override
			public String apply(Tokenable t) {
				return t.name();
			}
		}).collect(Collectors.toList()).toArray(new String[tokens.length]));
	}

	public ChainGrammer matchAnyToken(String... tokens) {
		chain.add(new MultiTokenGrammer(tokens, null));
		return this;
	}

	public ChainGrammer matchAny(Grammar... tokens) {
		chain.add(new MultiGrammer(Arrays.stream(tokens).map((a)->a.getName()).collect(Collectors.toList()).toArray(new String[tokens.length]), null));
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
	public ChainGrammer repeatMatchToken(Tokenable bracketOpen) {
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

	public ChainGrammer anyExcept(Tokenable token) {
		return anyExcept(token.name(), null);
	}

	
	public ChainGrammer anyExceptOneOf(String... tokens) {
		return anyExceptOneOf(null, tokens);
	}

	public ChainGrammer anyExceptOneOf(Tokenable... tokens) {
		return anyExceptOneOf(null, Arrays.stream(tokens).map((a)->a.name()).collect(Collectors.toList()).toArray(new String[tokens.length]));
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
		chain.add(new OptinalGrammer(value.getName(), null));
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
	public ChainGrammer matchTokenOptional(Tokenable token) {
		return matchTokenOptional(token.name());
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param value
	 * @return
	 */
	public ChainGrammer matchTokenOptional(String token) {
		chain.add(new OptionalTokenGrammer(token, null));
		return this;
	}
	
	
	

	/**
	 * @return the chain
	 */
	public ArrayList<Grammar> getChain() {
		return chain;
	}
	
	/*
	 * +-----<Title>-----+
	 * | USING REFERENCE |
	 * +-----------------+
	 */

	public ChainGrammer match(String g, String name) {
		chain.add(new GrammarMatchGrammer(g, name));
		return this;
	}

	public ChainGrammer ignore(String g) {
		chain.add(new IgnoreGrammer(g));
		return this;
	}

	public ChainGrammer matchAny(String name, String... tokens) {
		chain.add(new MultiGrammer(tokens, name));
		return this;
	}

	public ChainGrammer repeatMatch(String expression, String name) {
		chain.add(new RepeatGrammer(expression, name));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param value
	 * @return
	 */
	public ChainGrammer matchOptional(String value, String name) {
		chain.add(new OptinalGrammer(value, name));
		return this;
	}

	public ChainGrammer match(String g) {
		chain.add(new GrammarMatchGrammer(g, null));
		return this;
	}

	public ChainGrammer matchAny(String... tokens) {
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
	public ChainGrammer repeatMatch(String expression) {
		return repeatMatch(expression, null);
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param value
	 * @return
	 */
	public ChainGrammer matchOptional(String value) {
		chain.add(new OptinalGrammer(value, null));
		return this;
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#getExecutor()
	 */
	@Override
	public GrammarExecutor getExecutor() {
		return new ChainExecutor(chain);
	}
}
