package com.niton.parser.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import com.niton.parser.GrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.SubGrammerObject;
import com.niton.parser.Tokenizer.AssignedToken;
import com.niton.parser.Tokens;

/**
 * This is the ChainGrammer Class
 * 
 * @author Nils
 * @version 2019-05-28
 */
public class ChainGrammer extends Grammer {
	String name;
	ArrayList<Grammer> chain = new ArrayList<>();

	/**
	 * @throws ParsingException
	 * @see com.niton.parser.check.Grammer#check(java.util.ArrayList)
	 */
	@Override
	public GrammerObject process(ArrayList<AssignedToken> tokens) throws ParsingException {
		int pos = index();
		SubGrammerObject gObject = new SubGrammerObject();
		gObject.name = name;
		for (Grammer grammer : chain) {
			gObject.objects.add(grammer.check(tokens, pos));
			pos = grammer.index();
		}
		index(pos);
		return gObject;
	}

	public ChainGrammer name(String text) {
		this.name = text;
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
		return matchToken(number.name());
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-28
	 * @param string
	 * @return
	 */
	public ChainGrammer matchToken(String string) {
		chain.add(new TokenGrammer(string));
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
		return matchAnyToken(Arrays.stream(tokens).map(new Function<Tokens, String>() {

			@Override
			public String apply(Tokens t) {
				return t.name();
			}
		}).collect(Collectors.toList()).toArray(new String[tokens.length]));
	}

	public ChainGrammer matchAnyToken(String... tokens) {
		chain.add(new MultiTokenGrammer(tokens));
		return this;
	}

	public ChainGrammer match(Grammer g) {
		chain.add(g);
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
	public ChainGrammer repeatMatch(Grammer expression) {
		return repeatMatch(expression, "MULTIPLICITY");
	}

	public ChainGrammer repeatMatch(Grammer expression, String name) {
		chain.add(new RepeatGrammer(expression, name));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param name2
	 * @return
	 */
	public ChainGrammer ignore(Grammer name2) {
		chain.add(new IgnoreGrammer(name2));
		return this;
	}

	public ChainGrammer matchAny(Grammer... tokens) {
		chain.add(new MultiGrammer(tokens));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param string
	 * @return
	 */
	public ChainGrammer anyExcept(String string) {
		chain.add(new AnyExceptTokenGrammer(string));
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
	public ChainGrammer matchOptional(Grammer value) {
		chain.add(new OptinalGrammer(value));
		return this;
	}

	/**
	 * Description :
	 * 
	 * @author Nils
	 * @version 2019-05-29
	 * @param stringDelmitter
	 * @return
	 */
	public ChainGrammer anyExcept(Tokens stringDelmitter) {
		return anyExcept(stringDelmitter.name());
	}
}
