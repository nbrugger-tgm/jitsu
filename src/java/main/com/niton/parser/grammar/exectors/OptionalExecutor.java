package com.niton.parser.grammar.exectors;

import java.util.ArrayList;

import com.niton.parser.GrammarObject;
import com.niton.parser.GrammarReference;
import com.niton.parser.IgnoredGrammerObject;
import com.niton.parser.ParsingException;
import com.niton.parser.Tokenizer.AssignedToken;

/**
 * Cheks if the grammer is right if yes it adds the element to the output if not
 * it is ignored
 * 
 * @author Nils
 * @version 2019-05-29
 */
public class OptionalExecutor extends GrammarExecutor {
	
	private String check;

	public OptionalExecutor(String value, String name) {
		this.check = value;
		setName(name);
	}

	/**
	 * @see com.niton.parser.grammar.Grammar#process(java.util.ArrayList)
	 */
	@Override
	public GrammarObject process(ArrayList<AssignedToken> tokens,GrammarReference ref) throws ParsingException {
		try {
			GrammarExecutor g = ref.get(check).getExecutor();
			GrammarObject obj = g.check(tokens,index(),ref);
			if (obj == null)
				throw new ParsingException("");
			obj.setName(getName());
			index(g.index());
			return obj;
		} catch (Exception e) {
			return new IgnoredGrammerObject();
		}
	}
	

	/**
	 * @return the check
	 */
	public String getCheck() {
		return check;
	}

	/**
	 * @param check the check to set
	 */
	public void setCheck(String check) {
		this.check = check;
	}

}
