package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.TokenGrammarObject;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Cheks if the grammer is right if yes it adds the element to the output if not
 * it is ignored
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class OptionalTokenExecutor extends GrammarExecutor {
	private String token;
	
	public OptionalTokenExecutor(String token, String name) {
		this.token = token;
		setName(name);
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens,GrammarReference ref) throws ParsingException {
		TokenGrammarObject tgo = new TokenGrammarObject();
		tgo.setName(getName());
		if (tokens.get(index()).name.equals(token)) {
			tgo.tokens.add(tokens.get(index()));
			increase();
			return tgo;
		} else {
			return new IgnoredGrammerObject();
		}
	}
	
	public final GrammarObject check(ArrayList<AssignedToken> tokens, int pos,GrammarReference reference) throws ParsingException {
		index(pos);
		if (index() >= tokens.size())
			return new IgnoredGrammerObject();
		return process(tokens, reference);
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

}
